[![Build Status](https://travis-ci.org/19007361/green.svg?branch=master)](https://travis-ci.org/wvisser/green?branch=master)

Notes:

The first step is to update "build.properties" with your local
settings.  You do not need to set z3 and latte, but in that case
some unit tests won't run.
   
Task 1:

Hello so I changed the test20 in SATCanonizerTest so that the 3rd argument
(expected output) is the correct canonical form of the given expression

Task 2:

I added this to my .travis.yml:

sudo: required
services:
	- docker
before_install:
	- docker build -t meyer/java .
script:
	- docker run meyer/java /bin/sh -c "ant; ant test;"

	MY GITHUB FOLDER:
	https://github.com/19007361/green
