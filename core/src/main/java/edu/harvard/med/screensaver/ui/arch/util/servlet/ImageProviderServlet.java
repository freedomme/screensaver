// $HeadURL: $
// $Id: $
//
// Copyright © 2010 by the President and Fellows of Harvard College.
// 
// Screensaver is an open-source project developed by the ICCB-L and NSRB labs
// at Harvard Medical School. This software is distributed under the terms of
// the GNU General Public License.

package edu.harvard.med.screensaver.ui.arch.util.servlet;

import java.awt.Graphics;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;

import com.sun.image.codec.jpeg.JPEGCodec;
import com.sun.image.codec.jpeg.JPEGImageEncoder;

import edu.harvard.med.screensaver.ScreensaverConstants;
import edu.harvard.med.screensaver.ui.ApplicationInfo;
import edu.harvard.med.screensaver.util.StringUtils;

/**
 * This servlet allows access to images located outside the web application directory on the server.<br>
 * This is a spring-managed bean implementing the {@link Servlet} interface, intended for use with the
 * {@link DelegatingServletProxy}<br>
 * <br>
 * Function:<br>
 * <br>
 * Http requests to this servlet are be treated as image requests. The image is located by prepending a filesystem
 * location to the "extra path info" of the URL requested by the client. The image, if found, is written to the
 * response.
 * "extra path info" refers to the portion of the URL after the servlet name and before any query parameters, see
 * {@link HttpServletRequest#getPathInfo()}. <br>
 * <br>
 * <ul>
 * Usage:
 * <li>This is a Spring aware {@link Servlet} implementation that can be invoked from {@link DelegatingServletProxy}.
 * <li>This class must be instantiated in a Spring configuration file (i.e. spring-context.xml).
 * <li>The application property <b>&quot;screensaver.ui.imageproviderservlet.filesystempath&quot;</b> is required. This
 * should be {@link File#isAbsolute()}; however, if the path is relative, it will be interpreted as relative to the web
 * application root directory.
 * </ul>
 */
public class ImageProviderServlet extends HttpServlet
{
  private static final long serialVersionUID = 1L;
  private static Logger log = Logger.getLogger(ImageProviderServlet.class);

  private String _imagesFileSystemPath;
  private ApplicationInfo _appInfo;

  /**
   * @throws IllegalStateException if the application property
   *           <b>&quot;screensaver.ui.imageproviderservlet.filesystempath&quot;</b> is not defined.
   */
  public ImageProviderServlet(ApplicationInfo appInfo)
  {
    _appInfo = appInfo;
    _imagesFileSystemPath = appInfo.getApplicationProperties().getProperty(ScreensaverConstants.IMAGES_BASE_DIR);

    if (StringUtils.isEmpty(_imagesFileSystemPath)) {
      throw new IllegalStateException("The application property: " + ScreensaverConstants.IMAGES_BASE_DIR + " must be defined.");
    }
    log.debug("ImageProviderServlet: \"" + ScreensaverConstants.IMAGES_BASE_DIR + "\": " + _imagesFileSystemPath);
  }


  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
  {
    service(req, resp);
  }

  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
  {
    service(req, resp);
  }

  /**
   * Http requests to this servlet are be treated as image requests. The image is located by prepending a filesystem
   * location to the "extra path info" URL requested by the client. The image, if found, is written to the response.<br>
   * 
   * @throws IOException if the image file cannot be found
   */
  @Override
  protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
  {
    if (log.isDebugEnabled()) {
      log.debug("ImageProviderServlet: \"" + ScreensaverConstants.IMAGES_BASE_DIR + "\": " + _imagesFileSystemPath);
      log.debug("service: req.getPathInfo(): " + req.getPathInfo());
    }
    String pathInfo = req.getPathInfo();
    
    // FYI... setting the content type is not necessary for most (modern) browsers
    if (pathInfo.toLowerCase().matches(".*\\.png")) {
      resp.setContentType("image/png");
    }
    else if (pathInfo.toLowerCase().matches(".*\\.gif")) {
      resp.setContentType("image/gif");
    }
    else if (pathInfo.toLowerCase().matches(".*\\.bmp")) {
      resp.setContentType("image/bmp");
    }
    else if (pathInfo.toLowerCase().matches(".*\\.tiff")) {
      resp.setContentType("image/tiff");
    }
    else {
      resp.setContentType("image/jpeg"); // default should be ok, since not strictly req'd
    }

    if (pathInfo != null) {
      String temp = _imagesFileSystemPath;
      // make a relative path if its not absolute, this is not uber-useful, 
      // but it allows a *default* screensaver.properties to specify a valid system file path 
      // as simply a relative one, the actual deployment should specify absolute system paths, however.
      File file = new File(temp);
      if (!file.isAbsolute())
      {
        temp = req.getSession().getServletContext().getRealPath("/") + temp;
        file = new File(temp);
      }

      file = new File(file, pathInfo);
      if (log.isDebugEnabled()) {
        log.debug("retrieve the image: " + file.getAbsolutePath());
      }
      if(!file.exists()) {
        log.warn("image file not found: " + file);
        BufferedImage image  = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB );
        BufferedOutputStream bos=new BufferedOutputStream(resp.getOutputStream()); 
        JPEGImageEncoder encoder= JPEGCodec.createJPEGEncoder(bos);
        encoder.encode(image);
        return;
      }
      
      // note, allow IOException to be thrown if not found. (will print to the log)
      resp.getOutputStream().write(IOUtils.toByteArray(new FileInputStream(file)));
    }
    else {
      throw new ServletException("No image pathInfo specified");
    }
  }

  // TODO: add the ImageProviderServlet (as a spring-bean) to beans that serve images to enable performing this check - sde4
  public boolean canFindImage(URL url)
  {
      String baseUrl = _appInfo.getApplicationProperties().getProperty("screensaver.images.base_url");
      String relativePath = url.toString().substring(baseUrl.length());
      File file = new File(_imagesFileSystemPath, relativePath);
      if (!file.isAbsolute())
      {
        // if path is relative, bypass this check, since those paths are used during testing
        return true;
      }
      if(! file.exists()) {
        log.info("can't find the image at the url: " + url + ", mapped to the file: " + file);
        return false;
      }
      return true;
  }
}