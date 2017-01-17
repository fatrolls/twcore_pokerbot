cd ../../../..
pushd %MYPATH%
set MYPATH=%CD%
echo %MYPATH%
SET PATH=%PATH%;%MYPATH%\apache-ant-1.9.6\bin
SET JAVA_HOME=C:\Program Files\Java\jdk1.8.0_77
CALL ant botdir -Dbotdir=pokerbot
pause