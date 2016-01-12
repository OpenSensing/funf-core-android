package edu.mit.media.funf;

/**
 * Created by astopczynski@google.com on 1/11/16.
 * Provides Funf version when compilation is performed outside of git
 * or when Funf is exported to .jar instead of .aar (so resources are not exported).
 */
public class BuildInfo {
  //Should be set to current master version if development happens within git, otherwise dev version
  public final static String BUILD_INFO = "20160111-0.4.4-master";
}
