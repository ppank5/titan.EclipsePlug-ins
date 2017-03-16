package org.eclipse.titan.designer.compiler;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.titan.designer.GeneralConstants;
import org.eclipse.titan.designer.AST.MarkerHandler;
import org.eclipse.titan.designer.AST.Module;

/**
 * This is project level root of all java compiler related activities.
 * @author Arpad Lovassy
 */
public class ProjectSourceCompiler {

	/** the root package of the generated java source */
	private final static String PACKAGE_GENERATED_ROOT = "org.eclipse.titan.generated";
	private final static String PACKAGE_RUNTIME_ROOT = "org.eclipse.titan.runtime.core";
//	private final static String PACKAGE_RUNTIME_TYPES = PACKAGE_RUNTIME_ROOT + ".types";

	/** the root folder of the generated java source */
	private final static String DIR_GENERATED_ROOT = "java_src/org/eclipse/titan/generated";

	/**
	 * Generates java code for a module
	 * @param aModule module to compile
	 * @param aDebug true: debug info is added to the source code 
	 * @throws CoreException
	 */
	public static void compile( final Module aModule, final boolean aDebug ) throws CoreException {
		IResource sourceFile = aModule.getLocation().getFile();
		if(MarkerHandler.hasMarker(GeneralConstants.ONTHEFLY_SYNTACTIC_MARKER, sourceFile)) {
			// if there are syntactic errors in the module don't generate code for it
			// TODO semantic errors need to be checked for severity
			return;
		}
		
		JavaGenData data = new JavaGenData();
		data.setDebug( aDebug );
		aModule.generateJava( data );

		//write imports
		StringBuilder headerSb = new StringBuilder();
		writeHeader( headerSb, data );
		
		for(StringBuilder typeString: data.getTypes().values()) {
			data.getSrc().append(typeString);
		}

		writeFooter(data);
		

		//write src file body
		IProject project  = aModule.getProject();
		IFolder folder = project.getFolder( DIR_GENERATED_ROOT );
		IFile file = folder.getFile( aModule.getName() + ".java");
		createDir( folder );

		//write to file if changed
		final String content = headerSb.append(data.getSrc().toString()).toString();
		if (file.exists()) {
			if(needsUpdate(file, content) ) {
				final InputStream outputStream = new ByteArrayInputStream( content.getBytes() );
				file.setContents( outputStream, IResource.FORCE | IResource.KEEP_HISTORY, null );
			}
		} else {
			final InputStream outputStream = new ByteArrayInputStream( content.getBytes() );
			file.create( outputStream, IResource.FORCE, null );
		}
	}

	/**
	 * Compares the content of the file and the provided string content,
	 *  to determine if the file content needs to be updated or not.
	 * 
	 * @param file the file to check
	 * @param content the string to be generated if not already present in the file
	 * @return true if the file does not contain the provided string parameter
	 * */
	private static boolean needsUpdate(final IFile file, final String content) throws CoreException {
		boolean result = true;
		final InputStream filestream = file.getContents();
		final BufferedInputStream bufferedFile = new BufferedInputStream(filestream);
		final InputStream contentStream = new ByteArrayInputStream( content.getBytes() );
		final BufferedInputStream bufferedOutput = new BufferedInputStream(contentStream);
		try {
			int read1 = bufferedFile.read();
			int read2 = bufferedOutput.read();
			while (read1 != -1 && read1 == read2) {
				read1 = bufferedFile.read();
				read2 = bufferedOutput.read();
			}

			result = read1 != read2;
			bufferedFile.close();
			bufferedOutput.close();
		} catch (IOException exception) {
			return true;
		}

		return result;
	}


	/**
	 * RECURSIVE
	 * Creates full directory path
	 * @param aFolder directory to create
	 * @throws CoreException
	 */
	private static void createDir( IFolder aFolder ) throws CoreException {
		if (!aFolder.exists()) {
			createDir( (IFolder) aFolder.getParent() );
			aFolder.create( true, true, new NullProgressMonitor() );
		}
	}

	/**
	 * Builds header part of the java source file.
	 * <ul>
	 *   <li> header comment
	 *   <li> package
	 *   <li> includes
	 * </ul>
	 * @param aSb string buffer, where the result is written
	 * @param aData data collected during code generation, we need the include files form it
	 */
	private static void writeHeader( final StringBuilder aSb, final JavaGenData aData ) {
		aSb.append( "// This Java file was generated by the TITAN Designer eclipse plug-in\n" );
		aSb.append( "// of the TTCN-3 Test Executor version " ).append(GeneralConstants.VERSION_STRING).append('\n');
		aSb.append( "// for (").append(System.getProperty("user.name")).append('@');
		try {
			aSb.append(InetAddress.getLocalHost().getHostName());
		} catch (UnknownHostException e) {
			aSb.append("unknown");
		}
		//TODO date will need to be simplified to have optimize build time
//		aSb.append(") on ").append(new Date()).append("\n");
		aSb.append(")\n");
		aSb.append("\n" );
		aSb.append( "// ").append(GeneralConstants.COPYRIGHT_STRING).append("\n" );
		aSb.append("\n" );
		aSb.append( "// Do not edit this file unless you know what you are doing.\n" );
		aSb.append("\n" );
		aSb.append( "package " );
		aSb.append( PACKAGE_GENERATED_ROOT );
		aSb.append( ";\n\n" );
		
		for ( String importName : aData.getInternalImports() ) {
			aSb.append( "import " );
			aSb.append( PACKAGE_RUNTIME_ROOT );
			aSb.append( "." );
			aSb.append( importName );
			aSb.append( ";\n" );
		}

		for ( String importName : aData.getImports() ) {
			writeImport( aSb, importName );
		}
		aSb.append( "\n" );
	}
	
	/**
	 * Builds footer part of the java source file.
	 * <ul>
	 *   <li> pre init function: to initialize constants before module parameters are processed
	 *   <li> post init function: to initialize local "constants" after module parameters were processed.
	 * </ul>
	 * 
	 * @param aData data collected during code generation, we need the include files form it
	 */
	private static void writeFooter( final JavaGenData aData) {
		StringBuilder aSb = aData.getSrc();
		aSb.append("\n" );
		aSb.append("static void pre_init_module()").append("\n" );
		aSb.append("{").append("\n" );
		aSb.append(aData.getPreInit());
		aSb.append("").append("\n" );
		aSb.append("}").append("\n" );

		aSb.append("static void post_init_module()").append("\n" );
		aSb.append("{").append("\n" );
		aSb.append(aData.getPostInit());
		aSb.append("").append("\n" );	
		aSb.append("}").append("\n" );
		
		aSb.append("static boolean init_comp_type(final String component_type, final boolean init_base_comps)\n");
		aSb.append("{\n");
		aSb.append(aData.getInitComp());
		aSb.append("return false;\n");
		aSb.append("}\n");
		
		aSb.append( "}\n" );
	}

	/**
	 * Writes an import to the header
	 * @param aSb string buffer, where the result is written
	 * @param aImportName short class name to import. This function knows the package of all the runtime classes.
	 */
	private static void writeImport( final StringBuilder aSb, final String aImportName ) {
		aSb.append( "import " );
		aSb.append( aImportName );
		aSb.append( ";\n" );
	}
}
