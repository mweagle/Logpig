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

import ch.qos.logback.core.rolling.helper.CompressionMode;

import com.logpig.mweagle.rolling.helper.S3Compressor;

/**
 * Compression context object that encapsulates logback Compressor instance and source, target, entryName values.
 * 
 * @see CompressAndPostRunnable
 * @author Matt Weagle (mweagle@gmail.com)
 */
public class CompressionContext
{
	private final S3Compressor compressor;

	private final String sourceName;

	private final String targetName;

	private final String innerEntryName;

	/**
	 * Ctor
	 * 
	 * @param compressor
	 *            S3Compressor instance
	 * @param sourceName
	 *            Source filename
	 * @param targetName
	 *            Target (compressed) filename
	 * @param innerEntryName
	 *            Inner entry name for .zip archives
	 */
	public CompressionContext(S3Compressor compressor, String sourceName, String targetName, String innerEntryName)
	{
		this.compressor = compressor;
		this.sourceName = sourceName;
		this.targetName = targetName;
		this.innerEntryName = innerEntryName;
	}

	/**
	 * Compress the source file to the target name
	 * 
	 * @return The target (compressed) filename
	 */
	public String compress()
	{
		// Don't compress if the file mode isn't set

		// Check here since, the compressor will
		// append the extension iff it's not part of the target
		String compressedName = targetName;
		if (this.compressor.getMode() == CompressionMode.GZ)
		{
			compressor.compress(sourceName, targetName, innerEntryName);
			if (!targetName.toLowerCase().endsWith(".gz"))
			{
				compressedName += ".gz";
			}
		}
		else if (this.compressor.getMode() == CompressionMode.ZIP)
		{
			compressor.compress(sourceName, targetName, innerEntryName);
			if (!targetName.toLowerCase().endsWith(".zip"))
			{
				compressedName += ".zip";
			}
		}
		return compressedName;
	}
}
