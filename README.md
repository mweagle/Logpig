Logpig
======

Logpig is a [Logback](http://logback.qos.ch/) appender that automatically posts rolled over log files to S3 and a sample Pig script that illustrates computing aggregates and distributions of values extracted from the posted log files.

S3 recently added [object expiration](http://docs.amazonwebservices.com/AmazonS3/latest/dev/ObjectExpiration.html), which makes it a candidate to store log files with scheduled deletion.  

Logback Appender
-------
My goal was to make an Appender that worked like the existing RollingLogfileAppenders, but that would additionally send the rolled log files to S3.  

The resulting appender mimics Logback's current [appenders](http://logback.qos.ch/manual/appenders.html), and includes automatic posting on logifle rollover and Runtime shutdown.

To use the custom appender, in your __logback.xml__ file use the following classnames:

* appender 
	* class = com.logpig.mweagle.rolling.S3RollingFileAppender
* rollingPolicy
	* class = com.logpig.mweagle.rolling.S3TimeBasedRollingPolicy
* timeBasedFileNamingAndTriggeringPolicy
   * class = [ch.qos.logback.core.rolling.SizeAndTimeBasedFNATP](http://logback.qos.ch/apidocs/ch/qos/logback/core/rolling/SizeAndTimeBasedFNATP.html)

and add the three required properties as children of the &lt;appender&gt; element:

* accessKey - your S3 access key
* secretKey - your S3 secret key
* bucketName - the bucketName into which the rolled over log files will be posted

The appender does not set object expiration policies on the log file bucket.  You should manage expiration policies in the AWS management console.

For example:

NOTE* : This fragment (which is the one in the repo) WILL NOT WORK as is - you will need to change the *accessKey* and *secretKey* values to match your S3 credentials.  Visit the [AWS](https://aws-portal.amazon.com/gp/aws/developer/account/index.html/?action=access-key) page for more info.

Example logback.xml configuration:

    <appender name="R" class="com.logpig.mweagle.rolling.S3RollingFileAppender">

    	<File>./target/logs/logpig.log</File>
    
    	<!--  S3 Settings -->
    	<accessKey>MYACCESSKEY</accessKey>
    	<secretKey>MYSECRETKEY_MYSECRETKEY</secretKey>
		<bucketName>applogs</bucketName>
		<!--  S3 Settings -->
	
    	<encoder>
    	 <!--  Tab delimited so that Pig default loader can parse it without needing a UDF  -->
      	<pattern>%date{yyyy-MM-dd'T'HH:mm:ss.SSS,GMT}\t*%4p*\t%m%n</pattern>
    	</encoder>
    	<rollingPolicy class="com.logpig.mweagle.rolling.S3TimeBasedRollingPolicy">
     	 <!-- rollover daily -->
     	 	<fileNamePattern>./target/logs/logpig-%d{yyyy-MM-dd}.%i.log.gz</fileNamePattern>
       		<timeBasedFileNamingAndTriggeringPolicy class="ch.qos.logback.core.rolling.SizeAndTimeBasedFNATP">
        		<!-- Max 16kb size -->
        		<maxFileSize>16KB</maxFileSize>
      		</timeBasedFileNamingAndTriggeringPolicy>
    	</rollingPolicy>
    </appender>

Logpig's S3TimeBasedRollingPolicy respects the compression setting indicated by the suffix on the &lt;fileNamePattern&gt; value.  In the sample XML above, the log files are compressed after they are rolled over.  The S3TimeBasedRollingPolicy will post the compressed .GZ contents to S3 - which minimizes network traffic and works with Pig since the default can [directly load .GZ files](http://pig.apache.org/docs/r0.9.1/func.html#load-store-functions). 

See the com.logpig.mweagle.rolling.S3RollingFileAppender documentation for additional S3 options. 

Since log files are only posted to S3 when they are rolled over, choose appropriate values for the triggering policy in your __logback.xml__ configuration.  

Pig Script
----------
More an excuse to learn Pig than anything else, the included script demonstrates how to compute aggregates from the rolled log files.  

The com.logpig.mweagle.Main test runner generates 1000 log messages of the form:

    Guess %d is that the magic number is: %d

Where the first format specifier is the guess count and the second is in the range [0, 9].  

This is great, but aggregate values are more helpful.  [Apache Pig](http://pig.apache.org/) is a data flow language that can help compute aggregate values and can be run against the log files that have been uploaded to S3.  For example: 

Average value (__./pig/out/totals/*__)

    Total guesses:	1000	with an average value of: 4.583

Distribution of guesses, ordered by frequency (__./pig/out/guesses/*__):

    Guess:	8	was guessed:	116	, percentage: 0.116
    Guess:	5	was guessed:	108	, percentage: 0.108
    Guess:	6	was guessed:	102	, percentage: 0.102
    Guess:	0	was guessed:	99	, percentage: 0.099
    Guess:	4	was guessed:	99	, percentage: 0.099
    Guess:	7	was guessed:	97	, percentage: 0.097
    Guess:	9	was guessed:	96	, percentage: 0.096
    Guess:	1	was guessed:	95	, percentage: 0.095
    Guess:	2	was guessed:	95	, percentage: 0.095
    Guess:	3	was guessed:	93	, percentage: 0.093

The __pigTest.pig__ script includes __%default__ input paths that allow it to be run in local mode [local mode](http://ofps.oreilly.com/titles/9781449302641/running_pig.html) as well.  You'll need to [download](http://pig.apache.org/releases.html#Download) the Pig JAR file (v. 0.9.1) and make it available in your classpath.  

See also this [excellent whitepaper](http://aws.amazon.com/articles/2729) for more information on how to configure AWS to run your Pig scripts.

Building/Running
-------

Steps:

* Clone the repo
* cd com.logpig.mweagle
* mvn clean install
* mvn exec:java -Dexec.mainClass=com.logpig.mweagle.Main -Dexec.classpathScope="test"
* Download the [Apache Pig JAR](http://pig.apache.org/releases.html#Download) file and copy the JAR to the __pig__ folder
* cd ../pig
* Either execute the __pigtest.pig__ script using the Pig instructions, or via Java directly:
	* java -Xmx1024m -classpath pig-0.9.1.jar org.apache.pig.Main -x local pigTest.pig
* open ./out

To build the Javadocs:

* cd com.logpig.mweagle
* mvn javadoc:javadoc
* open target/site/apidocs/index.html 

