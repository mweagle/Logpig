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

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.model.Region;
/**
 * POJO to store Amazon S3 settings 
 *
 * @author Matt Weagle (mweagle@gmail.com)
 */
public class S3Settings {
	public final static int DEFAULT_RETRY_COUNT = 3;

	public final static Region DEFAULT_REGION_NAME = Region.US_Standard;

	public String bucketName;
	
	public String folderName;

	public String getFolderName() {
        return folderName;
    }

    public void setFolderName(String folderName) {
        this.folderName = folderName;
    }
    public Region regionName = S3Settings.DEFAULT_REGION_NAME;

	public boolean mockPut = false;

	public int retryCount = S3Settings.DEFAULT_RETRY_COUNT;

	public ArrayList<String> getPostSettingsErrors() {
		ArrayList<String>	errors = new ArrayList<String>();
		getSettingError("bucketName", bucketName, errors);
		getBucketNameErrors(errors);
		getRetryValueErrors(errors);
		return errors;
	}

	private void getSettingError(String name, String value, ArrayList<String> errors) {
		if (null == value || value.trim().isEmpty())
		{
			errors.add(String.format("%s property cannot be null", name));
		}
	}
	
	private void getRetryValueErrors(ArrayList<String> errors)
	{
		if (retryCount <= 0)
		{
			errors.add("Retry count must be between [1, Integer.MAX_VALUE]");
		}
	}
	private void getBucketNameErrors(ArrayList<String> errors)
	{
		/**
From http://docs.amazonwebservices.com/AWSJavaSDK/latest/javadoc/com/amazonaws/services/s3/AmazonS3Client.html#createBucket(java.lang.String, com.amazonaws.services.s3.model.Region)
Bucket names should not contain underscores
Bucket names should be between 3 and 63 characters long
Bucket names should not end with a dash
Bucket names cannot contain adjacent periods
Bucket names cannot contain dashes next to periods (e.g., "my-.bucket.com" and "my.-bucket" are invalid)
Bucket names cannot contain uppercase characters
		 */		
		if (null != bucketName)
		{
			if (bucketName.indexOf('_') != -1)
			{
				errors.add("Bucket names should not contain underscores");
			}
			if (bucketName.length() < 3 || bucketName.length() > 63)
			{
				errors.add("Bucket names should be between 3 and 63 characters long");
			}
			if (bucketName.endsWith("-"))
			{
				errors.add("Bucket names should not end with a dash");
			}
			if (bucketName.indexOf("..") != -1)
			{
				errors.add("Bucket names cannot contain adjacent periods");
			}
			if (bucketName.indexOf("-.") != -1 || bucketName.indexOf(".-") != -1)
			{
				errors.add("Bucket names cannot contain dashes next to periods");
			}
			if (!bucketName.equals(bucketName.toLowerCase()))
			{
				errors.add("Bucket names cannot contain uppercase characters");
			}			
		}
	}
}
