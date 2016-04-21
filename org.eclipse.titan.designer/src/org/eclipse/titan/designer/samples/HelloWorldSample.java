/******************************************************************************
 * Copyright (c) 2000-2015 Ericsson Telecom AB
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.eclipse.titan.designer.samples;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.titan.common.logging.ErrorReporter;
import org.eclipse.titan.designer.properties.data.ProjectBuildPropertyData;

/**
 * @author Szabolcs Beres
 * */
public class HelloWorldSample extends SampleProject {

	private static final String NEWLINE = System.getProperty("line.separator");

	private static final String NAME_TO_DISPLAY = "Hello World";
	private static final String DESCRIPTION = "TTCN-3 version of \"Hello, world!\"";

	/** The contents of the files. key - filename, value - content*/
	private static final Map<String, String> SOURCE_FILE_CONTENT = new HashMap<String, String>();

	private static final String MYEXAMPLE_TTCN =
			  "// TTCN-3 version of \"Hello, world!\"" + NEWLINE
			+ "module MyExample" + NEWLINE
			+ "{" + NEWLINE
			+ "type port PCOType message" + NEWLINE
			+ "{" + NEWLINE
			+ "  inout charstring;" + NEWLINE
			+ "}" + NEWLINE
			+ NEWLINE
			+ "type component MTCType" + NEWLINE
			+ "{" + NEWLINE
			+ "  port PCOType MyPCO_PT;" + NEWLINE
			+ "}" + NEWLINE
			+ NEWLINE
			+ "testcase tc_HelloW() runs on MTCType system MTCType" + NEWLINE
			+ "{" + NEWLINE
			+ "  map(mtc:MyPCO_PT, system:MyPCO_PT);" + NEWLINE
			+ "  MyPCO_PT.send(\"Hello, world!\");" + NEWLINE
			+ "  setverdict(pass);" + NEWLINE
			+ "}" + NEWLINE
			+ NEWLINE
			+ "testcase tc_HelloW2() runs on MTCType system MTCType" + NEWLINE
			+ "{" + NEWLINE
			+ "  timer TL_T := 15.0;" + NEWLINE
			+ "  map(mtc:MyPCO_PT, system:MyPCO_PT);" + NEWLINE
			+ "  MyPCO_PT.send(\"Hello, world!\");" + NEWLINE
			+ "  TL_T.start;" + NEWLINE
			+ "  alt {" + NEWLINE
			+ "    [] MyPCO_PT.receive(\"Hello, TTCN-3!\") { TL_T.stop; setverdict(pass); }" + NEWLINE
			+ "    [] TL_T.timeout { setverdict(inconc); }" + NEWLINE
			+ "    [] MyPCO_PT.receive { TL_T.stop; setverdict(fail); }" + NEWLINE
			+ "  }" + NEWLINE
			+ "}" + NEWLINE
			+ NEWLINE
			+ "control" + NEWLINE
			+ "{" + NEWLINE
			+ "  execute(tc_HelloW());" + NEWLINE
			+ "  execute(tc_HelloW2());" + NEWLINE
			+ "}" + NEWLINE
			+ "}" + NEWLINE;

	private static final String MYEXAMPLE_CFG =
			  "[LOGGING]" + NEWLINE
			+ "LogFile := \"../log/MyExample-%n.log\"" + NEWLINE
			+ "FileMask := LOG_ALL" + NEWLINE
			+ "ConsoleMask := ERROR | TESTCASE | STATISTICS" + NEWLINE
			+ "LogSourceInfo := Stack" + NEWLINE
			+ NEWLINE
			+ "[EXECUTE]" + NEWLINE
			+ "MyExample.control" + NEWLINE;

	// copy of titan/hello/PCOType.hh
	private static final String PCOTYPE_HH =
			  "// This Test Port skeleton header file was generated by the" + NEWLINE
			+ "// TTCN-3 Compiler of the TTCN-3 Test Executor version CRL 113 200/4 R2A" + NEWLINE
			+ "// for Arpad Lovassy (earplov@esekilxxen1841) on Tue Jul 22 16:49:55 2014" + NEWLINE
			+ NEWLINE
			+ "// Copyright (c) 2000-2015 Ericsson Telecom AB" + NEWLINE
			+ NEWLINE
			+ "// You may modify this file. Add your attributes and prototypes of your" + NEWLINE
			+ "// member functions here." + NEWLINE
			+ NEWLINE
			+ "#ifndef PCOType_HH" + NEWLINE
			+ "#define PCOType_HH" + NEWLINE
			+ NEWLINE
			+ "#include \"MyExample.hh\"" + NEWLINE
			+ NEWLINE
			+ "namespace MyExample {" + NEWLINE
			+ NEWLINE
			+ "class PCOType : public PCOType_BASE {" + NEWLINE
			+ "public:" + NEWLINE
			+ "	PCOType(const char *par_port_name = NULL);" + NEWLINE
			+ "	~PCOType();" + NEWLINE
			+ NEWLINE
			+ "	void set_parameter(const char *parameter_name," + NEWLINE
			+ "		const char *parameter_value);" + NEWLINE
			+ NEWLINE
			+ "	void Event_Handler(const fd_set *read_fds," + NEWLINE
			+ "		const fd_set *write_fds, const fd_set *error_fds," + NEWLINE
			+ "		double time_since_last_call);" + NEWLINE
			+ NEWLINE
			+ "private:" + NEWLINE
			+ "	/* void Handle_Fd_Event(int fd, boolean is_readable," + NEWLINE
			+ "		boolean is_writable, boolean is_error); */" + NEWLINE
			+ "	void Handle_Fd_Event_Error(int fd);" + NEWLINE
			+ "	void Handle_Fd_Event_Writable(int fd);" + NEWLINE
			+ "	void Handle_Fd_Event_Readable(int fd);" + NEWLINE
			+ "	/* void Handle_Timeout(double time_since_last_call); */" + NEWLINE
			+ "protected:" + NEWLINE
			+ "	void user_map(const char *system_port);" + NEWLINE
			+ "	void user_unmap(const char *system_port);" + NEWLINE
			+ NEWLINE
			+ "	void user_start();" + NEWLINE
			+ "	void user_stop();" + NEWLINE
			+ NEWLINE
			+ "	void outgoing_send(const CHARSTRING& send_par);" + NEWLINE
			+ "};" + NEWLINE
			+ NEWLINE
			+ "} /* end of namespace */" + NEWLINE
			+ NEWLINE
			+ "#endif";

	// copy of titan/hello/PCOType.cc
	private static final String PCOTYPE_CC =
			  "// This Test Port skeleton source file was generated by the" + NEWLINE
			+ "// TTCN-3 Compiler of the TTCN-3 Test Executor version CRL 113 200/4 R2A" + NEWLINE
			+ "// for Arpad Lovassy (earplov@esekilxxen1841) on Tue Jul 22 16:49:55 2014" + NEWLINE
			+ NEWLINE
			+ "// Copyright (c) 2000-2015 Ericsson Telecom AB" + NEWLINE
			+ NEWLINE
			+ "// You may modify this file. Complete the body of empty functions and"
			+ "// add your member functions here." + NEWLINE
			+ NEWLINE
			+ "#include \"PCOType.hh\"" + NEWLINE
			+ "#include \"memory.h\"" + NEWLINE
			+ NEWLINE
			+ "#include <stdio.h>" + NEWLINE
			+ NEWLINE
			+ "namespace MyExample {" + NEWLINE
			+ NEWLINE
			+ "PCOType::PCOType(const char *par_port_name)" + NEWLINE
			+ "	: PCOType_BASE(par_port_name)" + NEWLINE
			+ "{" + NEWLINE
			+ NEWLINE
			+ "}" + NEWLINE
			+ NEWLINE
			+ "PCOType::~PCOType()" + NEWLINE
			+ "{" + NEWLINE
			+ NEWLINE
			+ "}" + NEWLINE
			+ NEWLINE
			+ "void PCOType::set_parameter(const char * /*parameter_name*/," + NEWLINE
			+ "	const char * /*parameter_value*/)" + NEWLINE
			+ "{" + NEWLINE
			+ NEWLINE
			+ "}" + NEWLINE
			+ NEWLINE
			+ "void PCOType::Event_Handler(const fd_set *read_fds," + NEWLINE
			+ "	const fd_set *write_fds, const fd_set *error_fds," + NEWLINE
			+ "	double time_since_last_call)" + NEWLINE
			+ "{" + NEWLINE
			+ "	size_t buf_len = 0, buf_size = 32;" + NEWLINE
			+ "	char *buf = (char*)Malloc(buf_size);" + NEWLINE
			+ "	for ( ; ; ) {" + NEWLINE
			+ "		int c = getc(stdin);" + NEWLINE
			+ "		if (c == EOF) {" + NEWLINE
			+ "			if (buf_len > 0) incoming_message(CHARSTRING(buf_len, buf));" + NEWLINE
			+ "			Uninstall_Handler();" + NEWLINE
			+ "			break;" + NEWLINE
			+ "		} else if (c == '\\n') {" + NEWLINE
			+ "			incoming_message(CHARSTRING(buf_len, buf));" + NEWLINE
			+ "			break;" + NEWLINE
			+ "		} else {" + NEWLINE
			+ "			if (buf_len >= buf_size) {" + NEWLINE
			+ "				buf_size *= 2;" + NEWLINE
			+ "				buf = (char*)Realloc(buf, buf_size);" + NEWLINE
			+ "			}" + NEWLINE
			+ "			buf[buf_len++] = c;" + NEWLINE
			+ "		}" + NEWLINE
			+ "	}" + NEWLINE
			+ "	Free(buf);" + NEWLINE
			+ "}" + NEWLINE
			+ NEWLINE
			+ "/*void PCOType::Handle_Fd_Event(int fd, boolean is_readable," + NEWLINE
			+ "	boolean is_writable, boolean is_error) {}*/" + NEWLINE
			+ NEWLINE
			+ "void PCOType::Handle_Fd_Event_Error(int /*fd*/)" + NEWLINE
			+ "{" + NEWLINE
			+ NEWLINE
			+ "}" + NEWLINE
			+ NEWLINE
			+ "void PCOType::Handle_Fd_Event_Writable(int /*fd*/)" + NEWLINE
			+ "{" + NEWLINE
			+ NEWLINE
			+ "}" + NEWLINE
			+ NEWLINE
			+ "void PCOType::Handle_Fd_Event_Readable(int /*fd*/)" + NEWLINE
			+ "{" + NEWLINE
			+ NEWLINE
			+ "}" + NEWLINE
			+ NEWLINE
			+ "/*void PCOType::Handle_Timeout(double time_since_last_call) {}*/" + NEWLINE
			+ NEWLINE
			+ "void PCOType::user_map(const char *system_port)" + NEWLINE
			+ "{" + NEWLINE
			+ "	fd_set readfds;" + NEWLINE
			+ "	FD_ZERO(&readfds);" + NEWLINE
			+ "	FD_SET(fileno(stdin), &readfds);" + NEWLINE
			+ "	Install_Handler(&readfds, NULL, NULL, 0.0);" + NEWLINE
			+ "}" + NEWLINE
			+ NEWLINE
			+ "void PCOType::user_unmap(const char *system_port)" + NEWLINE
			+ "{" + NEWLINE
			+ "	Uninstall_Handler();" + NEWLINE
			+ "}" + NEWLINE
			+ NEWLINE
			+ "void PCOType::user_start()" + NEWLINE
			+ "{" + NEWLINE
			+ NEWLINE
			+ "}" + NEWLINE
			+ NEWLINE
			+ "void PCOType::user_stop()" + NEWLINE
			+ "{" + NEWLINE
			+ NEWLINE
			+ "}" + NEWLINE
			+ NEWLINE
			+ "void PCOType::outgoing_send(const CHARSTRING& send_par)" + NEWLINE
			+ "{" + NEWLINE
			+ "	puts((const char*)send_par);" + NEWLINE
			+ "	fflush(stdout);" + NEWLINE
			+ "}" + NEWLINE
			+ NEWLINE
			+ "} /* end of namespace */" + NEWLINE;

	static {
		SOURCE_FILE_CONTENT.put("MyExample.ttcn", MYEXAMPLE_TTCN);
		SOURCE_FILE_CONTENT.put("MyExample.cfg", MYEXAMPLE_CFG);
		SOURCE_FILE_CONTENT.put("PCOType.hh", PCOTYPE_HH);
		SOURCE_FILE_CONTENT.put("PCOType.cc", PCOTYPE_CC);
	}

	@Override
	public Map<String, String> getSourceFileContent() {
		return SOURCE_FILE_CONTENT;
	}

	@Override
	public String getName() {
		return NAME_TO_DISPLAY;
	}

	@Override
	public String getDescription() {
		return DESCRIPTION;
	}

	@Override
	protected void configure(final IProject project) {
		final String trueString = "true";
		try {
			project.setPersistentProperty(new QualifiedName(ProjectBuildPropertyData.QUALIFIER,
					ProjectBuildPropertyData.GENERATE_MAKEFILE_PROPERTY), trueString);
			project.setPersistentProperty(new QualifiedName(ProjectBuildPropertyData.QUALIFIER,
					ProjectBuildPropertyData.GENERATE_INTERNAL_MAKEFILE_PROPERTY), trueString);
			project.setPersistentProperty(new QualifiedName(ProjectBuildPropertyData.QUALIFIER,
					ProjectBuildPropertyData.SYMLINKLESS_BUILD_PROPERTY), trueString);
		} catch (CoreException e) {
			ErrorReporter.logExceptionStackTrace("Error while setting project property", e);
		}
	}
}
