package org.cantact.core;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import org.cantact.proto.IsotpInterface;
import org.cantact.proto.UdsClient;
import org.openide.windows.IOProvider;
import org.openide.windows.InputOutput;

public class ScriptManager implements CanListener {

    ScriptEngine engine;
    InputOutput io = IOProvider.getDefault().getIO("Scripting", true);
    Future<Object> scriptFuture;
    boolean isRunning;
    private final IsotpInterface isotpInterface;
    private final IsotpReceiveHandler isotpReceiveHandler;
    private final UdsClient udsClient;

    public ScriptManager() {
        isRunning = false;
        isotpReceiveHandler = new IsotpReceiveHandler();
        isotpInterface = new IsotpInterface(0, 0, isotpReceiveHandler);
        DeviceManager.addListener(isotpInterface);
        udsClient = new UdsClient(isotpInterface);
    }

    public void runScript(final String script) {
        // initialize the scripting engine
        engine = new ScriptEngineManager().getEngineByName("nashorn");
        // set the writer to the output console to direct print statements
        engine.getContext().setWriter(io.getOut());

        // put supporting objects into the script context
        engine.put("isotp", isotpInterface);
        engine.put("uds", udsClient);
        
        // create a future that evaluates the script
        final Callable<Object> c = new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                Object result = null;
                try {
                    // create variables for references to java classes
                    engine.eval("device = Java.type('org.cantact.core.DeviceManager');");
                    engine.eval("frame = Java.type('org.cantact.core.CanFrame');");
                    // run the user's script
                    result = engine.eval(script);
                } catch (ScriptException e) {
                    // log error into output console
                    io.getErr().println("Script Error: " + e.toString());
                    stopScript();
                }
                return result;
            }
        };
        // start the script
        scriptFuture = Executors.newCachedThreadPool().submit(c);
        isRunning = true;
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
            invocable.invokeFunction("onCanReceived", f);
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
            isotpInterface.reset();
            Invocable invocable = (Invocable) engine;
            try {
                // call script's callback function
                invocable.invokeFunction("stop");
            } catch (ScriptException e) {
                // log error into output console
                io.getErr().println("Script Error: " + e.toString());
            } catch (NoSuchMethodException e) {
                // no callback defined, do nothing
            }
        }
    }

    private class IsotpReceiveHandler implements IsotpInterface.IsotpCallback {
        @Override
        public void onIsotpReceived(int[] data) {
            // do nothing if not running script
            if (!isRunning) {
                return;
            }

            Invocable invocable = (Invocable) engine;
            // convert int[] to list of Integers
            List<Integer> dataList = new ArrayList<>();
            for (int i : data) {
                dataList.add(i);
            }
            try {
                // call script's callback function
                invocable.invokeFunction("onIsotpReceived", dataList);
            } catch (ScriptException e) {
                // log error into output console
                io.getErr().println("Script Error: " + e.toString());
            } catch (NoSuchMethodException e) {
                // no callback defined, do nothing
            }
        }
    }
}
