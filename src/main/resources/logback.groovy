import java.lang.management.ManagementFactory
import java.util.logging.Handler
import java.util.logging.LogManager

import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.encoder.PatternLayoutEncoder
import ch.qos.logback.classic.jul.LevelChangePropagator
import ch.qos.logback.classic.jmx.MBeanUtil
import ch.qos.logback.classic.jmx.JMXConfigurator
import ch.qos.logback.core.ConsoleAppender
import ch.qos.logback.core.status.OnConsoleStatusListener

import org.slf4j.bridge.SLF4JBridgeHandler

import static ch.qos.logback.classic.Level.INFO

// Always a good idea to add an on console status listener
statusListener(OnConsoleStatusListener)

scan("10 seconds")
context.name = "apache-tomcat"

// ------------------------------
// Taken from de http://www.mail-archive.com/logback-user@qos.ch/msg02978.html
// No native support by Logback converter

def contextListener = new LevelChangePropagator()
addInfo("Reseting JUL")
contextListener.resetJUL = true
// ------------------------------

// ------------------------------
// Register SLF4JBridgeHandler

addInfo("Registering SLF4JBridgeHandler")
java.util.logging.Logger rootLogger = LogManager.getLogManager().getLogger("");
Handler[] handlers = rootLogger.getHandlers();
if (handlers != null ) {
    for (Handler handler : handlers) {
        rootLogger.removeHandler(handler);
    }
}
SLF4JBridgeHandler.install();
// ------------------------------

// ------------------------------
// Taken from http://stackoverflow.com/questions/6232009/logback-groovy-config-to-use-jmx
// No native support by Logback converter
 
addInfo("Adding JMXConfigurator")
def jmxConfigurator() {
    def contextName = context.name
    def objectNameAsString = MBeanUtil.getObjectNameFor(contextName, JMXConfigurator.class)
    def objectName = MBeanUtil.string2ObjectName(context, this, objectNameAsString)
    def platformMBeanServer = ManagementFactory.getPlatformMBeanServer()
    if (!MBeanUtil.isRegistered(platformMBeanServer, objectName)) {
        JMXConfigurator jmxConfigurator = new JMXConfigurator((LoggerContext) context, platformMBeanServer, objectName)
        try {
            platformMBeanServer.registerMBean(jmxConfigurator, objectName)
        } catch (all) {
            addError("Failed to create mbean", all)
        }
    }
}

jmxConfigurator()
// ------------------------------

appender("console", ConsoleAppender) {
  encoder(PatternLayoutEncoder) {
    pattern = "[@/%contextName] %date{HH:mm:ss,SSS} %-5level %thread:%mdc{user}[%logger] %msg%n"
  }
}

root(INFO, ["console"])
