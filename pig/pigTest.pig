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
/** 
Defaults for properties that are overriden by S3 Settings
*/
%default INPUT '../com.logpig.mweagle/target/logs';
%default OUTPUT './out'

/* 
Cleanup the previous output 

fs -rmr $OUTPUT
fs -rm pig_*.log
fs -rm pig_*.log
*/
/*
Create the ordered list of magic number guesses
*/
rawLogData = LOAD '$INPUT' USING PigStorage('\t') AS (time, level, message);
/*
The log line that we care about is:
Guess %d is that the magic number is: %d
*/
allPossibleGuesses = FOREACH rawLogData GENERATE message, FLATTEN((INT)REGEX_EXTRACT (message, '.*(\\d+)$', 1)) AS guessValue:int;
filteredLogData = FILTER allPossibleGuesses BY NOT guessValue IS NULL;
totalAndAverage = FOREACH (GROUP filteredLogData ALL) GENERATE COUNT(filteredLogData), AVG(filteredLogData.guessValue);
groupedGuesses = GROUP filteredLogData BY guessValue;
guessCounts = FOREACH groupedGuesses GENERATE COUNT(filteredLogData), $0, (double)COUNT(filteredLogData) / (double) totalAndAverage.$0 as percentage;
orderedGuessCounts = ORDER guessCounts BY $0 DESC;
/**
Include the percentage
https://cwiki.apache.org/PIG/faq.html#FAQ-Q%253AHowcanIcalculateapercentage%2528partialaggregate%252Ftotalaggregate%2529%253F
*/
outputData = FOREACH orderedGuessCounts GENERATE 'Guess:', $1, 'was guessed:', $0, ', percentage: ', $2;
STORE outputData INTO '$OUTPUT/guesses' USING PigStorage('\t');

/**
Output some aggregates
*/
outputData = FOREACH totalAndAverage GENERATE 'Total guesses:', $0, 'with an average value of:', $1;
STORE outputData INTO '$OUTPUT/totals' USING PigStorage('\t');
