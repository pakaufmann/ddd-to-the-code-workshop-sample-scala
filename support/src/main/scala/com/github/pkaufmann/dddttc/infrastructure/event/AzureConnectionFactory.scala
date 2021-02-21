package com.github.pkaufmann.dddttc.infrastructure.event

import javax.jms.ConnectionFactory
import javax.naming.{Context, InitialContext}

object AzureConnectionFactory {
  def apply(url: String, password: String): ConnectionFactory = {
    val hashtable = new java.util.Hashtable[String, String]
    hashtable.put("connectionfactory.SBCF", url)
    hashtable.put("default.connectionfactory.username", "RootManageSharedAccessKey")
    hashtable.put("default.connectionfactory.password", password)
    hashtable.put(Context.INITIAL_CONTEXT_FACTORY, "org.apache.qpid.jms.jndi.JmsInitialContextFactory")
    val context = new InitialContext(hashtable)
    context.lookup("SBCF").asInstanceOf[ConnectionFactory]
  }
}
