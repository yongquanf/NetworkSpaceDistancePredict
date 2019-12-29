package edu.NUDT.PDL.measurement;


import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.Socket;
import java.net.UnknownHostException;

import javax.net.*;
//import javax.net.SocketFactory;
/**
 * Specialized SocketFactory to create sockets with a SOCKS proxy
 */
public class SocksSocketFactory extends SocketFactory{


  private Proxy proxy;

   
  /**
   * Default empty constructor (for use with the reflection API).
   */
  public SocksSocketFactory() {
    this.proxy = Proxy.NO_PROXY;
  }

  /**
   * Constructor with a supplied Proxy
   * 
   * @param proxy the proxy to use to create sockets
   */
  public SocksSocketFactory(Proxy proxy) {
    this.proxy = proxy;
  }

  /**
   * construct with a supplied proxy string
   * @param proxyStr
   */
  public SocksSocketFactory(String proxyStr) {
	    this.setProxy(proxyStr);
	    }

  
  
  public Socket createSocket() throws IOException {

    return new Socket(proxy);
  }


  public Socket createSocket(InetAddress addr, int port) throws IOException {

    Socket socket = createSocket();
    socket.connect(new InetSocketAddress(addr, port));
    return socket;
  }


  public Socket createSocket(InetAddress addr, int port,
      InetAddress localHostAddr, int localPort) throws IOException {

    Socket socket = createSocket();
    socket.bind(new InetSocketAddress(localHostAddr, localPort));
    socket.connect(new InetSocketAddress(addr, port));
    return socket;
  }


  public Socket createSocket(String host, int port) throws IOException,
      UnknownHostException {

    Socket socket = createSocket();
    socket.connect(new InetSocketAddress(host, port));
    return socket;
  }


  public Socket createSocket(String host, int port,
      InetAddress localHostAddr, int localPort) throws IOException,
      UnknownHostException {

    Socket socket = createSocket();
    socket.bind(new InetSocketAddress(localHostAddr, localPort));
    socket.connect(new InetSocketAddress(host, port));
    return socket;
  }

  /* @inheritDoc */
  @Override
  public int hashCode() {
    return proxy.hashCode();
  }

  /* @inheritDoc */
  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (!(obj instanceof SocksSocketFactory))
      return false;
    final SocksSocketFactory other = (SocksSocketFactory) obj;
    if (proxy == null) {
      if (other.proxy != null)
        return false;
    } else if (!proxy.equals(other.proxy))
      return false;
    return true;
  }



 

  /**
   * Set the proxy of this socket factory as described in the string
   * parameter
   * 
   * @param proxyStr the proxy address using the format "host:port"
   */
  private void setProxy(String proxyStr) {
    String[] strs = proxyStr.split(":", 2);
    if (strs.length != 2)
      throw new RuntimeException("Bad SOCKS proxy parameter: " + proxyStr);
    String host = strs[0];
    int port = Integer.parseInt(strs[1]);
    this.proxy =
        new Proxy(Proxy.Type.SOCKS, InetSocketAddress.createUnresolved(host,
            port));
  }
}
