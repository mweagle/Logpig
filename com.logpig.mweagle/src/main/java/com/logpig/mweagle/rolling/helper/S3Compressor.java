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
package com.logpig.mweagle.rolling.helper;

import ch.qos.logback.core.rolling.helper.CompressionMode;
import ch.qos.logback.core.rolling.helper.Compressor;

public class S3Compressor extends Compressor
{
	final private CompressionMode mode;

	public S3Compressor(CompressionMode compressionMode)
	{
		super(compressionMode);
		mode = compressionMode;
	}

	public CompressionMode getMode()
	{
		return mode;
	}
}
