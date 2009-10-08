##################################################
# JIST (Java In Simulation Time) Project
# Timestamp: <Makefile Tue 2005/03/08 18:59:59 barr rimbase.rimonbarr.com>
#

# Copyright (C) 2004 by Cornell University
# All rights reserved.
# Refer to LICENSE for terms and conditions of use.


# build programs and flags
CLASSPATH:=../libs/bcel.jar:../libs/bsh.jar:../libs/jargs.jar:../libs/log4j.jar:../libs/jython.jar:.:$(CLASSPATH)
JAVA=java
JAVAC=javac
JAVAC_FLAGS=-g -source 1.4 -classpath $(CLASSPATH)
#JAVAC=jikes
#JAVAC_FLAGS=-source 1.4 +P -g -classpath $(CLASSPATH)
RMIC=rmic
RMIC_FLAGS=-v1.2 -d . -g -classpath $(CLASSPATH)
JAVADOC=javadoc
JAVADOC_FLAGS=-source 1.4 -quiet -breakiterator -sourcepath .:/usr/java/src -classpath $(CLASSPATH) -splitindex -version -author -private -doctitle "JiST (Java in Simulation Time) - API Documentation"

# source files
JIST_SRC:= \
	$(wildcard jist/runtime/*.java) \
	$(wildcard jist/runtime/guilog/*.java) \

SWANS_SRC:= \
	$(wildcard jist/swans/*.java) \
	$(wildcard jist/swans/field/*.java) \
	$(wildcard jist/swans/radio/*.java) \
	$(wildcard jist/swans/mac/*.java) \
	$(wildcard jist/swans/net/*.java) \
	$(wildcard jist/swans/route/*.java) \
	$(wildcard jist/swans/trans/*.java) \
	$(wildcard jist/swans/app/*.java) \
	$(wildcard jist/swans/app/io/*.java) \
	$(wildcard jist/swans/app/net/*.java) \
	$(wildcard jist/swans/app/lang/*.java) \
	$(wildcard jist/swans/misc/*.java) \
	$(wildcard jist/swans/gui/*.java) \

APP_SRC:= \
	$(wildcard jist/minisim/*.java) \
	$(wildcard driver/*.java) \


JIST_PKG:= \
	jist.runtime \
	jist.runtime.guilog \

SWANS_PKG:= \
	jist.swans \
	jist.swans.field \
	jist.swans.radio \
	jist.swans.mac \
	jist.swans.net \
	jist.swans.route \
	jist.swans.trans \
	jist.swans.app \
	jist.swans.app.io \
	jist.swans.app.net \
	jist.swans.misc \

APP_PKG:= \
	jist.minisim \
	driver \

empty:=
space:=$(empty) $(empty)

RMI := \
	jist.runtime.Controller \
	jist.runtime.RemoteIO.RemoteInputStreamSender \
	jist.runtime.RemoteIO.RemoteOutputStreamReceiver \
	jist.runtime.RemoteJist.Ping \
	jist.runtime.RemoteJist.JistClient \
	jist.runtime.RemoteJist.JobQueueServer \

RMI_CLASS := \
	jist/runtime/Controller.class \
	jist/runtime/RemoteIO$$RemoteInputStreamSender.class \
	jist/runtime/RemoteIO$$RemoteOutputStreamReceiver.class \
	jist/runtime/RemoteJist$$Ping.class \
	jist/runtime/RemoteJist$$JistClient.class \
	jist/runtime/RemoteJist$$JobQueueServer.class \

JIST_OBJ:=$(JIST_SRC:.java=.class)
SWANS_OBJ:=$(SWANS_SRC:.java=.class)
APP_OBJ:=$(APP_SRC:.java=.class)
RMI_STUB_CLASS:=$(RMI_CLASS:.class=_Stub.class)

# targets
.PHONY: all clean jist swans app swansjni docs rmi jar

all: jist swans app rmi jar

help:
	@echo "Usage: make {all|clean|jist|rmi|swans|doc}"

clean:
	@make -C jist/swans/misc clean
	@find . -name \*.class -print0 | xargs -0 rm -f
	@find . -name \*_Stub.java -print0 | xargs -0 rm -f
	@rm -rf docs docs.stamp

jist: $(JIST_OBJ)

swans: jist $(SWANS_OBJ) swansjni

swansjni: $(SWANS_OBJ) $(APP_OBJ)
	@-make -C jist/swans/misc

app: jist swans $(APP_OBJ)

rmi: jist $(RMI_STUB_CLASS)

# build rules
$(JIST_OBJ): $(JIST_SRC)
	$(JAVAC) $(JAVAC_FLAGS) $(JIST_SRC)
	@touch $(JIST_OBJ)

$(SWANS_OBJ): $(SWANS_SRC)
	$(JAVAC) $(JAVAC_FLAGS) $(SWANS_SRC)
	@touch $(SWANS_OBJ)

$(APP_OBJ): $(APP_SRC)
	$(JAVAC) $(JAVAC_FLAGS) $(APP_SRC)
	@touch $(APP_OBJ)

$(RMI_STUB_CLASS): $(RMI_CLASS)
	$(RMIC) $(RMIC_FLAGS) $(RMI)

#%.class: %.java
#	$(JAVAC) $(JAVAC_FLAGS) $<

# doc
docs: docs.stamp

docs.stamp:
	$(JAVADOC) $(JAVADOC_FLAGS) -d docs \
	  -group JiST $(subst $(space),:,$(strip $(JIST_PKG))) \
	  -group SWANS $(subst $(space),:,$(strip $(SWANS_PKG))) \
	  -group Apps $(subst $(space),:,$(strip $(APP_PKG))) \
		$(JIST_PKG) $(SWANS_PKG) $(APP_PKG)
	@touch docs.stamp

style:
	@find . -name \*.java -print0 | xargs -0 $(JAVA) -cp ../libs/checkstyle-all.jar:$(CLASSPATH) com.puppycrawl.tools.checkstyle.Main -c ../libs/style.xml

