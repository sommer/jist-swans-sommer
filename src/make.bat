@set CP=..\libs\bcel.jar;..\libs\bsh.jar;..\libs\jargs.jar;..\libs\log4j.jar;..\libs\jython.jar;.
@set OPT=-g -source 1.4 -classpath %CP%

javac %OPT% jist\runtime\*.java jist\runtime\guilog\*.java
javac %OPT% jist\minisim\*.java
javac %OPT% jist\swans\*.java jist\swans\field\*.java jist\swans\radio\*.java jist\swans\mac\*.java jist\swans\net\*.java jist\swans\route\*.java jist\swans\trans\*.java jist\swans\app\*.java jist\swans\misc\*.java jist\swans\app\net\*.java jist\swans\app\io\*.java
javac %OPT% driver\*.java
rmic -v1.2 -d . -g -classpath %CP% jist.runtime.Controller jist.runtime.RemoteIO.RemoteInputStreamSender jist.runtime.RemoteIO.RemoteOutputStreamReceiver jist.runtime.RemoteJist.Ping jist.runtime.RemoteJist.JistClient jist.runtime.RemoteJist.JobQueueServer
