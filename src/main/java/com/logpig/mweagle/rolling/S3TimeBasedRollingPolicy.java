/*
   Copyright 2012 Matt Weagle (mweagle@gmail.com)

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */
package com.logpig.mweagle.rolling;

import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import ch.qos.logback.core.rolling.RolloverFailure;
import ch.qos.logback.core.rolling.TimeBasedFileNamingAndTriggeringPolicy;
import ch.qos.logback.core.rolling.helper.ArchiveRemover;
import ch.qos.logback.core.rolling.helper.CompressionMode;
import ch.qos.logback.core.rolling.helper.FileFilterUtil;
import ch.qos.logback.core.rolling.helper.RenameUtil;

import com.logpig.mweagle.aws.S3FilePutRunnable;
import com.logpig.mweagle.rolling.helper.S3Compressor;

/**
 * TimeBasedRollingPolicy subclass that supports forwarding rolled over files
 * to Amazon S3.  Includes a Runtime shutdown Runnable to forcibly rollover 
 * open logfiles using same compression strategy and forward to S3.  
 * 
 * This class duplicates a bit of parent class functionality to gain access to 
 * values which otherwise are package private.
 *
 * @author Matt Weagle (mweagle@gmail.com)
 */
public class S3TimeBasedRollingPolicy<E> extends ch.qos.logback.core.rolling.TimeBasedRollingPolicy<E>
{
	static final int NO_DELETE_HISTORY = 0;

	private S3Compressor compressor;

	private RenameUtil renameUtil = new RenameUtil();

	private S3Settings s3Settings;

	@SuppressWarnings("unused")
	private Future<?> future;

	private ArchiveRemover archiveRemover;

	public void start()
	{
		/**
		 * It would be more consistent to make the start call the last call in this method, but the compressionMode
		 * setting is initialized as part of the superclass start, and we need that to initialize our own
		 * S3Compressor
		 */
		super.start();
		// Setup the compressor
		compressor = new S3Compressor(super.getCompressionMode());
		compressor.setContext(context);

		// The rename util
		renameUtil.setContext(super.getContext());

		// the maxHistory property is given to TimeBasedRollingPolicy instead of to
		// the TimeBasedFileNamingAndTriggeringPolicy. This makes it more convenient
		// for the user at the cost of inconsistency here.
		if (super.getMaxHistory() != NO_DELETE_HISTORY)
		{
			archiveRemover = super.getTimeBasedFileNamingAndTriggeringPolicy().getArchiveRemover();
			archiveRemover.setMaxHistory(super.getMaxHistory());
		}
	}

	public void rollover() throws RolloverFailure
	{
		String elapsedPeriodsFileName = super.getTimeBasedFileNamingAndTriggeringPolicy().getElapsedPeriodsFileName();

		String elapsedPeriodStem = FileFilterUtil.afterLastSlash(elapsedPeriodsFileName);

		if (compressionMode == CompressionMode.NONE)
		{
			if (getParentsRawFileProperty() != null)
			{
				renameUtil.rename(getParentsRawFileProperty(), elapsedPeriodsFileName);
				future = this.futureUncompressedPost(elapsedPeriodsFileName);
			} // else { nothing to do if CompressionMode == NONE and parentsRawFileProperty == null }
		}
		else
		{
			if (getParentsRawFileProperty() == null)
			{
				future = futureAsyncCompressAndPost(elapsedPeriodsFileName, elapsedPeriodsFileName, elapsedPeriodStem);
			}
			else
			{
				future = renamedRawAndAsyncFutureCompressAndPost(elapsedPeriodsFileName, elapsedPeriodStem);
			}
		}
		if (archiveRemover != null)
		{
			archiveRemover.clean(new Date(super.getTimeBasedFileNamingAndTriggeringPolicy().getCurrentTime()));
		}
	}

	public void rolloverOnJVMShutdown() throws RolloverFailure
	{
		// when rollover is called the elapsed period's file has
		// been already closed. This is a working assumption of this method.
		final TimeBasedFileNamingAndTriggeringPolicy<?> triggerPolicy = super
				.getTimeBasedFileNamingAndTriggeringPolicy();
		final String periodFileName = triggerPolicy.getCurrentPeriodsFileNameWithoutCompressionSuffix();
		final String parentsRawFilename = getParentsRawFileProperty();
		final String elapsedPeriodStem = FileFilterUtil.afterLastSlash(periodFileName);
		final String activeFile = this.getActiveFileName();

		String extension = "";
		if (CompressionMode.GZ == super.getCompressionMode())
		{
			extension = ".gz";
		}
		else if (CompressionMode.ZIP == super.getCompressionMode())
		{
			extension = ".zip";
		}
		final String rolloverFilename = String.format("%s%s", periodFileName, extension);

		String rolloverName = null;
		if (CompressionMode.NONE == super.getCompressionMode())
		{
			if (null != parentsRawFilename)
			{
				final RenameUtil renameUtil = new RenameUtil();
				renameUtil.setContext(super.getContext());
				renameUtil.rename(parentsRawFilename, rolloverFilename);
				rolloverName = rolloverFilename;
			}
		}
		else
		{
			if (null != parentsRawFilename)
			{
				compressor.compress(activeFile, rolloverFilename, elapsedPeriodStem);
				rolloverName = rolloverFilename;
			}
			else
			{
				compressor.compress(activeFile, activeFile, elapsedPeriodStem);
				rolloverName = activeFile;
			}
		}
		// Post it...
		final S3FilePutRunnable s3Poster = new S3FilePutRunnable(rolloverName, this.getS3Settings());
		s3Poster.run();
	}

	@SuppressWarnings("rawtypes")
	Future futureUncompressedPost(String fileName)
	{
		ExecutorService executor = Executors.newScheduledThreadPool(1);
		final PostUncompressedRunnable postRunnable = new PostUncompressedRunnable(fileName, this.s3Settings);
		Future<?> future = executor.submit(postRunnable);
		executor.shutdown();
		return future;
	}

	@SuppressWarnings("rawtypes")
	Future futureAsyncCompressAndPost(String nameOfFile2Compress, String nameOfCompressedFile, String innerEntryName)
			throws RolloverFailure
	{
		final CompressionContext context = new CompressionContext(this.compressor, nameOfFile2Compress,
				nameOfCompressedFile, innerEntryName);
		final CompressAndPostRunnable compressAndPost = new CompressAndPostRunnable(context, this.s3Settings);

		ExecutorService executor = Executors.newScheduledThreadPool(1);
		Future<?> future = executor.submit(compressAndPost);
		executor.shutdown();
		return future;
	}

	@SuppressWarnings("rawtypes")
	Future renamedRawAndAsyncFutureCompressAndPost(String nameOfCompressedFile, String innerEntryName)
			throws RolloverFailure
	{
		String parentsRawFile = getParentsRawFileProperty();
		String tmpTarget = parentsRawFile + System.nanoTime() + ".tmp";
		renameUtil.rename(parentsRawFile, tmpTarget);
		return futureAsyncCompressAndPost(tmpTarget, nameOfCompressedFile, innerEntryName);
	}

	public S3Settings getS3Settings()
	{
		return s3Settings;
	}

	public void setS3Settings(S3Settings s3Settings)
	{
		this.s3Settings = s3Settings;
		Runtime.getRuntime().addShutdownHook(new Thread(new ShutdownHookRunnable(this)));
	}

	/**
	 * Runnable that posts a local, uncompressed file to S3
	 *
	 * @author Matt Weagle (mweagle@gmail.com)
	 */
	private static class PostUncompressedRunnable implements Runnable
	{
		private final String filePath;
		private final S3Settings awsSettings;

		/**
		 * Ctor
		 * @param filePath		Path to file that should be posted
		 * @param settings		S3Settings instance 
		 */
		public PostUncompressedRunnable(String filePath, S3Settings settings)
		{
			this.filePath = filePath;
			this.awsSettings = settings;
		}

		@Override
		public void run()
		{
			final S3FilePutRunnable postFile = new S3FilePutRunnable(this.filePath, this.awsSettings);
			postFile.run();
		}
	}
	/**
	 * Runnable invoked as part of JVM shutdown that calls back into 
	 * {@link S3TimeBasedRollingPolicy#rolloverOnJVMShutdown()} to close
	 * the open file, compress it, and post it to S3.
	 */
	private static class ShutdownHookRunnable implements Runnable
	{
		private final S3TimeBasedRollingPolicy<?> s3RollingPolicy;

		/**
		 * Ctor
		 * @param s3RollingPolicy	The S3TimeBasedRollingPolicy that is managing the open 
		 * 							logfile
		 */
		public ShutdownHookRunnable(S3TimeBasedRollingPolicy<?> s3RollingPolicy)
		{
			this.s3RollingPolicy = s3RollingPolicy;
		}

		@Override
		public void run()
		{
			try
			{
				this.s3RollingPolicy.rolloverOnJVMShutdown();
			}
			catch (RolloverFailure e)
			{
				e.printStackTrace();
			}
		}
	}
}
