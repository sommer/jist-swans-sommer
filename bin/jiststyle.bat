@set JAVA=java
@set JAVA_OPT=-server
@rem set JAVA="c:\program files\ibm\java13\bin\java"
@rem set JAVA_OPT=

@set CP=..\libs\checkstyle-all.jar;..\libs\jargs.jar;..\libs\log4j.jar;..\src;%CLASSPATH%
@%JAVA% %JAVA_OPT% -classpath %CP% com.puppycrawl.tools.checkstyle.Main -c ..\libs\style.xml %1 %2 %3 %4 %5 %6 %7 %8 %9
