/* JAI-Ext - OpenSource Java Advanced Image Extensions Library
 *    http://www.geo-solutions.it/
 *    Copyright 2018 GeoSolutions
 *    
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


/* 
 *  Copyright (c) 2009-2013, Michael Bedward. All rights reserved. 
 *   
 *  Redistribution and use in source and binary forms, with or without modification, 
 *  are permitted provided that the following conditions are met: 
 *   
 *  - Redistributions of source code must retain the above copyright notice, this  
 *    list of conditions and the following disclaimer. 
 *   
 *  - Redistributions in binary form must reproduce the above copyright notice, this 
 *    list of conditions and the following disclaimer in the documentation and/or 
 *    other materials provided with the distribution.   
 *   
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND 
 *  ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED 
 *  WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE 
 *  DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR 
 *  ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES 
 *  (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; 
 *  LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON 
 *  ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT 
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS 
 *  SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. 
 */   

package it.geosolutions.jaiext.jiffle;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.TokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.codehaus.janino.SimpleCompiler;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import it.geosolutions.jaiext.jiffle.parser.ExpressionWorker;
import it.geosolutions.jaiext.jiffle.parser.ImagesBlockWorker;
import it.geosolutions.jaiext.jiffle.parser.InitBlockWorker;
import it.geosolutions.jaiext.jiffle.parser.JiffleLexer;
import it.geosolutions.jaiext.jiffle.parser.JiffleParser;
import it.geosolutions.jaiext.jiffle.parser.JiffleParserErrorListener;
import it.geosolutions.jaiext.jiffle.parser.Messages;
import it.geosolutions.jaiext.jiffle.parser.OptionsBlockWorker;
import it.geosolutions.jaiext.jiffle.parser.RuntimeModelWorker;
import it.geosolutions.jaiext.jiffle.parser.SourcePositionsWorker;
import it.geosolutions.jaiext.jiffle.parser.VarWorker;
import it.geosolutions.jaiext.jiffle.parser.node.GetSourceValue;
import it.geosolutions.jaiext.jiffle.parser.node.Script;
import it.geosolutions.jaiext.jiffle.parser.node.SourceWriter;
import it.geosolutions.jaiext.jiffle.runtime.JiffleDirectRuntime;
import it.geosolutions.jaiext.jiffle.runtime.JiffleIndirectRuntime;
import it.geosolutions.jaiext.jiffle.runtime.JiffleRuntime;

/**
 * Compiles scripts and generates Java sources and executable bytecode for
 * runtime classes.
 * <p>
 * Example of use:
 * <pre><code>
 * // A script to write sequential values to image pixels
 * String script = "images { dest=write; } dest = x() + y() * width();" ;
 *
 * Jiffle jiffle = new Jiffle();
 * jiffle.setScript(script);
 * jifle.compile();
 *
 * // Now we get the runtime object from the compiled Jiffle object
 * JiffleDirectRuntime runtime = jiffle.getRuntimeInstance();
 *
 * // Create an image to hold the results of the script
 * final int width = 10;
 * WritableRenderedImage destImg = ImageUtils.createConstantImage(width, width, 0.0d);
 * 
 * // Associate this image with the variable name used in the script
 * runtime.setDestinationImage("dest", destImg);
 *
 * // Evaluate the script for all destination image pixels
 * runtime.evaluateAll();
 * </code></pre>
 * For further examples of how to create and run Jiffle scripts see the
 * {@code it.geosolutions.jaiext.demo.jiffle} package in the JAI-tools demo module.
 *
 * <h4>Implementation note</h4>
 * The Jiffle compiler is actually a Jiffle to Java translator.
 * When a client requests a runtime object, the script is translated into 
 * Java source for a runtime class. This source code is then passed to an 
 * embedded Janino compiler which produces the runtime object.
 *
 * @see JiffleBuilder
 * @see it.geosolutions.jaiext.jiffle.runtime.JiffleExecutor
 * 
 * @author Michael Bedward
 * @since 0.1
 * @version $Id$
 */
public class Jiffle {
    
    public static final Logger LOGGER = Logger.getLogger(Jiffle.class.getName());
    
    private static Pattern BLOCK_COMMENT_STRIPPER = Pattern.compile("(?:/\\*(?:[^*]|(?:\\*+[^*/]))*\\*+/)");

    /** 
     * Constants for runtime model. Jiffle supports two runtime models:
     * <ol type="1">
     * <li>
     * <b>Direct</b> - where the runtime class {@code evaluate} method directly 
     * sets values in the destination image(s)
     * </li>
     * <li>
     * <b>Indirect</b> - where there is a single destination image and the runtime
     * class {@code evaluate} method returns the destination value, leaving
     * it up to the caller to set the value in the image.
     * object 
     * </li>
     * </ol>
     * The indirect model is designed for use in an image operator or similar
     * context where further processing of destination values might be required
     * before writing to an image.
     */
    public static enum RuntimeModel {
        /** The runtime class implements {@link JiffleDirectRuntime} */
        DIRECT(JiffleDirectRuntime.class),
        
        /** The runtime class implements {@link JiffleIndirectRuntime} */
        INDIRECT(JiffleIndirectRuntime.class);
        
        private Class<? extends JiffleRuntime> runtimeClass;
        
        private RuntimeModel(Class<? extends JiffleRuntime> clazz) {
            this.runtimeClass = clazz;
        }

        /**
         * Gets the runtime interface.
         *
         * @return the runtime interface
         */
        public Class<? extends JiffleRuntime> getRuntimeClass() {
            return runtimeClass;
        }
        
        /**
         * Gets the matching constant for the given runtime class.
         *
         * @param clazz a runtime class
         *
         * @return the contant or {@code null} if the class does not derive
         *         from a supported base class
         */
        public static Jiffle.RuntimeModel get(Class<? extends JiffleRuntime> clazz) {
            for (Jiffle.RuntimeModel t : Jiffle.RuntimeModel.values()) {
                if (t.runtimeClass.isAssignableFrom(clazz)) {
                    return t;
                }
            }
            
            return null;
        }
    }

    
    /** Number of Jiffle instances */
    private static int refCount = 0;
    

    /**
     * Used to specify the roles of images referenced in
     * a Jiffle script. An image may be either read-only
     * ({@link Jiffle.ImageRole#SOURCE}) or write-only
     * ({@link Jiffle.ImageRole#DEST}) but not both.
     */
    public static enum ImageRole {
        /** Indicates an image is used for input (read-only) */
        SOURCE,
        
        /** Indicates an image is used for output (write-only) */
        DEST;
    }

    /** A name: either a default or one set by the client */
    private String name;

    private String theScript;
    private Script scriptModel;
    private Map<String, Jiffle.ImageRole> imageParams;
    private Map<String, Integer> destinationBands;
    
    /**
     * Creates a new instance.
     */
    public Jiffle() {
        init();
    }
    
    /**
     * Creates a new instance by compiling the provided script. Using this
     * constructor is equivalent to:
     * <pre><code>
     * Jiffle jiffle = new Jiffle();
     * jiffle.setScript(script);
     * jiffle.setImageParams(params);
     * jiffle.compile();
     * </code></pre>
     * 
     * @param script Jiffle source code to compile
     * 
     * @param params defines the names and roles of image variables
     *        referred to in the script.
     * @throws JiffleException on compilation errors
     * 
     */
    public Jiffle(String script, Map<String, Jiffle.ImageRole> params)
            throws it.geosolutions.jaiext.jiffle.JiffleException {
        init();
        setScript(script);
        setImageParams(params);
        compile();
    }

    /**
     * Creates a new instance by compiling the script read from {@code scriptFile}. 
     * Using this constructor is equivalent to:
     * <pre><code>
     * Jiffle jiffle = new Jiffle();
     * jiffle.setScript(scriptFile);
     * jiffle.setImageParams(params);
     * jiffle.compile();
     * </code></pre>
     * 
     * @param scriptFile file containing the Jiffle script
     * 
     * @param params defines the names and roles of image variables
     *        referred to in the script.
     * 
     * @throws JiffleException on compilation errors
     */
    public Jiffle(File scriptFile, Map<String, Jiffle.ImageRole> params)
            throws it.geosolutions.jaiext.jiffle.JiffleException {
        init();
        setScript(scriptFile);
        setImageParams(params);
        compile();
    }
    
    /**
     * Sets the script. Calling this method will clear any previous script
     * and runtime objects.
     * 
     * @param script a Jiffle script
     * @throws JiffleException if the script is empty or {@code null}
     */
    public final void setScript(String script) throws
            it.geosolutions.jaiext.jiffle.JiffleException {
        if (script == null || script.trim().length() == 0) {
            throw new it.geosolutions.jaiext.jiffle.JiffleException("script is empty !");
        }
        
        clearCompiledObjects();
        
        // add extra new line just in case last statement hits EOF
        theScript = script + "\n";
    }

    /**
     * Sets the script. Calling this method will clear any previous script
     * and runtime objects.
     * 
     * @param scriptFile a file containing a Jiffle script
     * @throws JiffleException if the script is empty or {@code null}
     */
    public final void setScript(File scriptFile) throws
            it.geosolutions.jaiext.jiffle.JiffleException {
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(scriptFile));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.length() > 0) {
                    sb.append(line);
                    sb.append('\n');  // put the newline back on for the parser
                }
            }
            
            setScript(sb.toString());
            
        } catch (IOException ex) {
            throw new it.geosolutions.jaiext.jiffle.JiffleException("Could not read the script file", ex);

        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException ignored) {
                }
            }
        }
    }
    
    /**
     * Gets the Jiffle script.
     * 
     * @return the script or an empty {@code String} if none
     *         has been set
     */
    public String getScript() {
        return theScript == null ? "" : theScript;
    }
    
    /**
     * Sets the image parameters. These define which variables in
     * the script refer to images and their types (source or destination).
     * <p>
     * This may be called before or after setting the script. No check is
     * made between script and parameters until the script is compiled.
     * 
     * @param params the image parameters
     */
    public final void setImageParams(Map<String, Jiffle.ImageRole> params) {
        imageParams.clear();
        imageParams.putAll(params);
    }
    
    /**
     * Gets the current image parameters. The parameters are returned
     * as an unmodifiable map.
     * 
     * @return image parameters or an empty {@code Map} if none
     *         are set
     */
    public Map<String, Jiffle.ImageRole> getImageParams() {
        return Collections.unmodifiableMap(imageParams);
    }

    /**
     * Replaces the default name set for this object with a user-supplied name.
     * The name is solely for use by client code. No checks are made for 
     * duplication between current instances.
     * 
     * @param name the name to assign
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Gets the name assigned to this object. This will either be the
     * default name or one assigned by the client via {@link #setName(String)}
     *
     * @return the name
     */
    public String getName() {
        return name;
    }
    
    /**
     * Compiles the script into Java source for the runtime class.
     * 
     * @throws JiffleException on compilation errors
     */
    public final void compile() throws it.geosolutions.jaiext.jiffle.JiffleException {
        if (theScript == null) {
            throw new it.geosolutions.jaiext.jiffle.JiffleException("No script has been set");
        }
        
        Jiffle.Result<ParseTree> parseResult = parseScript(theScript);
        if (parseResult.messages.isError()) {
            reportMessages(parseResult);
            return;
        }
        
        /*
         * If image var parameters were provided by the caller we
         * ignore any in the script. Otherwise, we look for an 
         * images block in the script.
         */
        ParseTree tree = parseResult.result;
        if (imageParams.isEmpty()) {
            Jiffle.Result<Map<String, Jiffle.ImageRole>> r = getScriptImageParams(
                    tree);
            if (r.messages.isError()) {
                reportMessages(r);
                return;
            }
            if (r.result.isEmpty()) {
                throw new it.geosolutions.jaiext.jiffle.JiffleException(
                        "No image parameters provided and none found in script");
            }
            
            setImageParams(r.result);
        }

        // run the other preparation and check related workers
        OptionsBlockWorker optionsWorker = new OptionsBlockWorker(tree);
        reportMessages(optionsWorker.messages);
        InitBlockWorker initWorker = new InitBlockWorker(tree);
        reportMessages(initWorker.messages);
        VarWorker vw = new VarWorker(tree, imageParams);
        ExpressionWorker expressionWorker = new ExpressionWorker(tree, vw);
        reportMessages(expressionWorker.messages);

        // 
        RuntimeModelWorker worker = new RuntimeModelWorker(tree, optionsWorker.options, expressionWorker.getProperties(), expressionWorker.getScopes());
        this.scriptModel = worker.getScriptNode();
        this.destinationBands = worker.getDestinationBands();
    }
    
    /**
     * Tests whether the script has been compiled successfully.
     *
     * @return {@code true} if the script has been compiled;
     *         {@code false} otherwise
     */
    public boolean isCompiled() {
        return scriptModel != null;
    }
    
    /**
     * Creates an instance of the default runtime class. 
     * <p>
     * The default runtime class implements {@link JiffleDirectRuntime} and
     * extends an abstract base class provided by the Jiffle compiler. Objects
     * of this class evaluate the Jiffle script and write results directly to
     * the destination image(s). Client code can call either of the methods:
     * <ul>
     * <li>{@code evaluate(int x, int y)}
     * <li>{@code evaluateAll(JiffleProgressListener listener}
     * </ul>
     * The {@code Jiffle} object must be compiled before calling this method.
     * 
     * @return the runtime object
     * @throws JiffleException if the script has not been compiled or if errors
     *         occur in creating the runtime instance
     */
    public JiffleDirectRuntime getRuntimeInstance() throws
            it.geosolutions.jaiext.jiffle.JiffleException {
        return (JiffleDirectRuntime) getRuntimeInstance(RuntimeModel.DIRECT);
    }
    
    /**
     * Creates a runtime object based using the class specified by {@code model}.
     * <p>
     * The {@code Jiffle} object must be compiled before calling this method.
     * 
     * @param model the {@link Jiffle.RuntimeModel}
     * @return the runtime object
     * @throws JiffleException  if the script has not been compiled or if errors
     *         occur in creating the runtime instance
     */
    public JiffleRuntime getRuntimeInstance(Jiffle.RuntimeModel model) throws
            it.geosolutions.jaiext.jiffle.JiffleException {
        return createRuntimeInstance(model, getRuntimeBaseClass(model), false);
    }

    /**
     * Gets the runtime object for this script. 
     * <p>
     * The runtime object is an instance of {@link JiffleRuntime}. By default
     * it extends an abstract base class supplied JAI-tools: 
     * {@link it.geosolutions.jaiext.jiffle.runtime.AbstractDirectRuntime}
     * when using the direct runtiem model or 
     * {@link it.geosolutions.jaiext.jiffle.runtime.AbstractIndirectRuntime}
     * when using the indirect model. This method allows you to
     * specify a custom base class. The custom class must implement either 
     * {@link JiffleDirectRuntime} or {@link JiffleIndirectRuntime}.
     *
     * @param <T> the runtime base class type
     * @param baseClass the runtime base class
     *
     * @return the runtime object
     * @throws JiffleException  if the script has not been compiled or if errors
     *         occur in creating the runtime instance
     */

    public <T extends JiffleRuntime> T getRuntimeInstance(Class<T> baseClass) throws
            it.geosolutions.jaiext.jiffle.JiffleException {
        RuntimeModel model = RuntimeModel.get(baseClass);
        if (model == null) {
            throw new it.geosolutions.jaiext.jiffle.JiffleException(baseClass.getName() +
                    " does not implement a required Jiffle runtime interface");
        }

        return (T) createRuntimeInstance(model, baseClass, false);
    }

    private JiffleRuntime createRuntimeInstance(RuntimeModel model, Class<? extends JiffleRuntime> runtimeClass, boolean scriptInDocs) throws
            
            it.geosolutions.jaiext.jiffle.JiffleException {
        if (!isCompiled()) {
            throw new it.geosolutions.jaiext.jiffle.JiffleException("The script has not been compiled");
        }

        String runtimeSource = createRuntimeSource(model, runtimeClass.getName(), scriptInDocs);
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.log(Level.FINE, "Jiffle script compiled to:\n\n" + runtimeSource);    
        }
        try {
            SimpleCompiler compiler = new SimpleCompiler();
            compiler.cook(runtimeSource);

            StringBuilder sb = new StringBuilder();
            sb.append(it.geosolutions.jaiext.jiffle.JiffleProperties.get(
                    it.geosolutions.jaiext.jiffle.JiffleProperties.RUNTIME_PACKAGE_KEY)).append(".");

            switch (model) {
                case DIRECT:
                    sb.append(it.geosolutions.jaiext.jiffle.JiffleProperties.get(
                            it.geosolutions.jaiext.jiffle.JiffleProperties.DIRECT_CLASS_KEY));
                    break;

                case INDIRECT:
                    sb.append(it.geosolutions.jaiext.jiffle.JiffleProperties.get(
                            it.geosolutions.jaiext.jiffle.JiffleProperties.INDIRECT_CLASS_KEY));
                    break;

                default:
                    throw new IllegalArgumentException("Internal compiler error");
            }

            Class<?> clazz = compiler.getClassLoader().loadClass(sb.toString());
            JiffleRuntime runtime = (JiffleRuntime) clazz.newInstance();
            runtime.setImageParams(imageParams);
            if (runtime instanceof JiffleIndirectRuntime) {
                ((JiffleIndirectRuntime) runtime).setDestinationBands(destinationBands);
            }
            return runtime;

        } catch (Exception ex) {
            throw new it.geosolutions.jaiext.jiffle.JiffleException("Runtime source error for source: " + runtimeSource, ex);
        }
    }

    /**
     * Gets a copy of the Java source for the runtime class. The 
     * script must have been compiled before calling this method.
     * 
     * @param scriptInDocs whether to include the original Jiffle script
     *        in the class javadocs
     * 
     * @return source for the runtime class
     * 
     * @throws JiffleException  if the script has not been compiled or if errors
     *         occur in creating the runtime source code
     */
    public String getRuntimeSource(boolean scriptInDocs)
            throws it.geosolutions.jaiext.jiffle.JiffleException {

        return getRuntimeSource(RuntimeModel.DIRECT, scriptInDocs);
    }
        
    /**
     * Gets a copy of the Java source for the runtime class. The 
     * script must have been compiled before calling this method.
     * 
     * @param model the {@link Jiffle.RuntimeModel}
     * @param scriptInDocs whether to include the original Jiffle script
     *        in the class javadocs
     * 
     * @return source for the runtime class
     * @throws JiffleException   if the script has not been compiled or if errors
     *         occur in creating the runtime source code
     * 
     */
    public String getRuntimeSource(Jiffle.RuntimeModel model, boolean scriptInDocs)
            throws it.geosolutions.jaiext.jiffle.JiffleException {
        return createRuntimeSource(model, getRuntimeBaseClass(model).getName(), scriptInDocs);
    }

    private Class<? extends JiffleRuntime> getRuntimeBaseClass(RuntimeModel model) {
        Class<? extends JiffleRuntime> baseClass = null;
        switch (model) {
            case DIRECT:
                baseClass = it.geosolutions.jaiext.jiffle.JiffleProperties.DEFAULT_DIRECT_BASE_CLASS;
                break;

            case INDIRECT:
                baseClass = it.geosolutions.jaiext.jiffle.JiffleProperties.DEFAULT_INDIRECT_BASE_CLASS;
                break;
        }
        
        return baseClass;
    }

    private String createRuntimeSource(RuntimeModel model, String baseClassName, boolean scriptInDocs) {
        if (scriptInDocs) {
            throw new RuntimeException("Do no know how to clean the block comments yet");
        }

        SourceWriter writer = new SourceWriter(model);
        writer.setScript(stripComments(theScript));
        writer.setBaseClassName(baseClassName);
        scriptModel.write(writer);
        return writer.getSource();
    }

    private String stripComments(String theScript) {
        return BLOCK_COMMENT_STRIPPER.matcher(theScript).replaceAll("");
    }

    /**
     * Initializes this object's name and runtime base class.
     */
    private void init() {
        Jiffle.refCount++ ;
        
        // TODO - CollectionFactory
        imageParams = new HashMap<String, Jiffle.ImageRole>();
    }
    
    /**
     * Clears all compiler and runtime objects.
     */
    private void clearCompiledObjects() {
        // TODO if necessary
    }
    
    /**
     * Builds the parse tree from the script.
     * @param script
     */
    private static Jiffle.Result<ParseTree> parseScript(String script) {
        CharStream input = CharStreams.fromString(script);
        
        JiffleLexer lexer = new JiffleLexer(input);
        TokenStream tokens = new CommonTokenStream(lexer);
        
        JiffleParser parser = new JiffleParser(tokens);
        parser.removeErrorListeners();
        
        JiffleParserErrorListener errListener = new JiffleParserErrorListener();
        parser.addErrorListener(errListener);

        ParseTree tree = parser.script();
        return new Jiffle.Result(tree, errListener.messages);
    }
    
    private static Jiffle.Result<Map<String, Jiffle.ImageRole>> getScriptImageParams(ParseTree tree) {
        ImagesBlockWorker reader = new ImagesBlockWorker(tree);
        return new Jiffle.Result(reader.imageVars, reader.messages);
    }

    private static void reportMessages(Jiffle.Result result) throws
            it.geosolutions.jaiext.jiffle.JiffleException {
        reportMessages(result.messages);
    }

    private static void reportMessages(Messages messages) throws
            it.geosolutions.jaiext.jiffle.JiffleException {
        if (messages.isError()) {
            String expectionMessage = messages.toString();
            throw new it.geosolutions.jaiext.jiffle.JiffleException(expectionMessage);
        }
    }
    
    private static class Result<T> {
        final T result;
        final Messages messages;

        public Result(T result, Messages messages) {
            this.result = result;
            this.messages = messages;
        }
    }

    /**
     * A utility method returning the source positions used in a given script
     *
     * @param script
     * @return
     */
    public static Set<GetSourceValue> getReadPositions(String script, List<String> sourceImageNames)
            throws JiffleException {
        Jiffle.Result<ParseTree> parseResult = parseScript(script);
        if (parseResult.messages.isError()) {
            reportMessages(parseResult);
            return Collections.emptySet();
        }

        // If image var parameters were provided by the caller we  ignore any in the script.
        // Otherwise, we look for an images block in the script.
        ParseTree tree = parseResult.result;
        if (sourceImageNames == null) {
            Jiffle.Result<Map<String, Jiffle.ImageRole>> r = getScriptImageParams(tree);
            sourceImageNames =
                    r.result
                            .entrySet()
                            .stream()
                            .filter(k -> k.getValue() == ImageRole.SOURCE)
                            .map(k -> k.getKey())
                            .collect(Collectors.toList());
        }

        if (sourceImageNames.isEmpty()) {
            return Collections.emptySet();
        }

        SourcePositionsWorker worker = new SourcePositionsWorker(tree, sourceImageNames);
        Set<GetSourceValue> positions = worker.getPositions();

        return positions;
    }
}
