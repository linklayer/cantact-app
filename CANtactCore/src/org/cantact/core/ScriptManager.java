/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.cantact.core;

import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import org.openide.windows.IOProvider;
import org.openide.windows.InputOutput;

/**
 *
 * @author eric
 */
public class ScriptManager implements CanListener {

    ScriptEngine engine;
    InputOutput io = IOProvider.getDefault().getIO("Scripting", true);
    Future<Object> scriptFuture;
    boolean isRunning;

    public ScriptManager() {
        // initialize the scripting engine
        engine = new ScriptEngineManager().getEngineByName("nashorn");
        // set the writer to the output console to direct print statements
        engine.getContext().setWriter(io.getOut());
        isRunning = false;
    }

    public void runScript(final String script) {

        isRunning = true;
        final Callable<Object> c = new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                Object result = null;
                try {
                    result = engine.eval(script);
                } catch (ScriptException e) {
                    // log error into output console
                    io.getErr().println("Script Error: " + e.toString());
                    stopScript();
                }
                return result;
            }
        };
        scriptFuture = Executors.newCachedThreadPool().submit(c);
        
    }

    @Override
    public void canReceived(CanFrame f) {
        // do nothing if not running script
        if (!isRunning) {
            return;
        }

        Invocable invocable = (Invocable) engine;
        try {
            // call script's callback function
            invocable.invokeFunction("canReceived", f);
        } catch (ScriptException e) {
            // log error into output console
            io.getErr().println("Script Error: " + e.toString());
        } catch (NoSuchMethodException e) {
            // no callback defined, do nothing 
        }
    }

    public void stopScript() {
        if (isRunning) {
            isRunning = false;
            scriptFuture.cancel(true);
        }
    }

}
