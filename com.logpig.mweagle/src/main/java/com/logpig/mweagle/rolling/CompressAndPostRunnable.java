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

import com.logpig.mweagle.aws.S3FilePutRunnable;

/**
 * Runnable that compresses a local file using a CompressionContext and then puts the compressed file to S3.
 * 
 * @see S3FilePutRunnable
 * @author Matt Weagle (mweagle@gmail.com)
 */
public class CompressAndPostRunnable implements Runnable
{
	private final CompressionContext context;

	private final S3Settings s3Settings;

	/**
	 * Ctor
	 * 
	 * @param context
	 *            CompressionContext instance that will compress the file if necessary
	 * @param s3Settings
	 *            S3Settings data
	 */
	public CompressAndPostRunnable(CompressionContext context, S3Settings s3Settings)
	{
		this.context = context;
		this.s3Settings = s3Settings;
	}

	@Override
	public void run()
	{
		final String compressedFilename = context.compress();
		final S3FilePutRunnable s3Poster = new S3FilePutRunnable(compressedFilename, s3Settings);
		s3Poster.run();
	}
}
