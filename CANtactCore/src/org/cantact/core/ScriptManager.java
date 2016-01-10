/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.cantact.core;

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
    InputOutput io = IOProvider.getDefault().getIO ("Scripting", true);
    boolean isRunning;
    
    public ScriptManager() {
        // initialize the scripting engine
        engine = new ScriptEngineManager().getEngineByName("nashorn");
        // set the writer to the output console to direct print statements
        engine.getContext().setWriter(io.getOut());
        isRunning = false;
    }
    
    public void runScript(String script) {
        try {
            isRunning = true;
            engine.eval(script);
        } catch (ScriptException e) {
            // log error into output console
            io.getErr().println("Script Error: " + e.toString());
            isRunning = false;
        }
    }
    
    @Override
    public void canReceived(CanFrame f) {
        // do nothing if not running script
        if (!isRunning) return;
        
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
        isRunning = false;
    }
    
}