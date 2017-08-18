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

import java.util.ArrayList;

import ch.qos.logback.core.rolling.RollingFileAppender;

import com.amazonaws.services.s3.model.Region;

/**
 * Logback compatible rolling logfile appender that supports the additional logback.xml properties for S3 configuration:
 * <ul>
 * <li><b>accessKey</b>: The S3 access key (String)</li>
 * <li><b>secretKey</b>: The S3 secret key (String)</li>
 * <li><b>bucketName</b>: The S3 bucket name into which logfiles should be added (String).  The appender optimistically
 * assumes this bucketName already exists, but will create it if a 404 is returned when adding files to S3.</li>
 * <li><b>regionName</b> (optional): The AWS Region name to use when posting logfiles (String). The String value must be
 * a valid <a href=
 * "http://docs.amazonwebservices.com/AWSJavaSDK/latest/javadoc/com/amazonaws/services/s3/model/Region.html#fromValue(java.lang.String)"
 * >Region value</a>. Defaults to <i>Region.US_Standard</i>.</li>
 * <li><b>retryCount</b> (optional): Number of times to retry (int). Must be greater than zero.  Defaults to {@link S3Settings#DEFAULT_RETRY_COUNT}</li>
 * <li><b>mockPost</b> (optional): If true, the appender will not submit files to S3, but will log the filenames that
 * would have been submitted</li>
 * </ul>
 * 
 * @author Matt Weagle (mweagle@gmail.com)
 */
public class S3RollingFileAppender<E> extends RollingFileAppender<E>
{
	private S3Settings s3Settings = new S3Settings();

	@Override
	public void start()
	{
		try
		{
			final ArrayList<String> errors = s3Settings.getPostSettingsErrors();
			if (errors.isEmpty())
			{
				final S3TimeBasedRollingPolicy<?> s3Policy = (S3TimeBasedRollingPolicy<?>) (super.getRollingPolicy());
				s3Policy.setS3Settings(s3Settings);
			}
			else
			{
				for (final String eachError : errors)
				{
					writeErrorMessage(eachError);
				}
			}
		}
		catch (ClassCastException ex)
		{
			writeErrorMessage("S3TimeBasedRollingPolicy not provided to S3RollingFileAppender");
			writeErrorMessage("Logs will not be forwarded to S3");
		}
		super.start();
	}

	public void setFolderName(String folderName)
	{
	    s3Settings.folderName = folderName;
	}
	
	public String getFolderName()
	{
	    return s3Settings.folderName;
	}

	public void setBucketName(String bucketName)
	{
		s3Settings.bucketName = bucketName;
	}

	public String getBucketName()
	{
		return s3Settings.bucketName;
	}

	public void setRegionName(String regionName)
	{
		s3Settings.regionName = Region.fromValue(regionName);
	}

	public String getRegionName()
	{
		return s3Settings.regionName.toString();
	}

	public void setRetryCount(int retryCount)
	{
		s3Settings.retryCount = retryCount;
	}

	public int getRetryCount()
	{
		return s3Settings.retryCount;
	}

	public void setMockPut(boolean mockPost)
	{
		s3Settings.mockPut = mockPost;
	}

	public boolean getMockPut()
	{
		return s3Settings.mockPut;
	}

	private void writeErrorMessage(String message)
	{
		System.err.println("[ERROR] S3RollingFileAppender - " + message);
	}
}
