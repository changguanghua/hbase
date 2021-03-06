/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.hadoop.hbase.util;

import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hbase.HConstants;
import org.apache.hadoop.hbase.HRegionInfo;
import org.apache.hadoop.hbase.regionserver.HRegion;
import org.apache.hadoop.hbase.regionserver.HStore;

/**
 * Helper class for all utilities related to archival/retrieval of HFiles
 */
public class HFileArchiveUtil {
  static final String DEFAULT_HFILE_ARCHIVE_DIRECTORY = ".archive";

  private HFileArchiveUtil() {
    // non-external instantiation - util class
  }

  /**
   * Get the directory to archive a store directory
   * @param conf {@link Configuration} to read for the archive directory name
   * @param region parent region information under which the store currently
   *          lives
   * @param family name of the family in the store
   * @return {@link Path} to the directory to archive the given store or
   *         <tt>null</tt> if it should not be archived
   */
  public static Path getStoreArchivePath(Configuration conf, HRegion region, byte [] family){
    return getStoreArchivePath(conf, region.getRegionInfo(), region.getTableDir(), family);
  }

  /**
   * Get the directory to archive a store directory
   * @param conf {@link Configuration} to read for the archive directory name. Can be null.
   * @param region parent region information under which the store currently lives
   * @param tabledir directory for the table under which the store currently lives
   * @param family name of the family in the store
   * @return {@link Path} to the directory to archive the given store or <tt>null</tt> if it should
   *         not be archived
   */
  public static Path getStoreArchivePath(Configuration conf, HRegionInfo region, Path tabledir,
      byte[] family) {
    Path tableArchiveDir = getTableArchivePath(conf, tabledir);
    return HStore.getStoreHomedir(tableArchiveDir,
      HRegionInfo.encodeRegionName(region.getRegionName()), family);
  }

  /**
   * Get the archive directory for a given region under the specified table
   * @param conf {@link Configuration} to read the archive directory from. Can be null
   * @param tabledir the original table directory. Cannot be null.
   * @param regiondir the path to the region directory. Cannot be null.
   * @return {@link Path} to the directory to archive the given region, or <tt>null</tt> if it
   *         should not be archived
   */
  public static Path getRegionArchiveDir(Configuration conf, Path tabledir, Path regiondir) {
    // get the archive directory for a table
    Path archiveDir = getTableArchivePath(conf, tabledir);

    // then add on the region path under the archive
    String encodedRegionName = regiondir.getName();
    return HRegion.getRegionDir(archiveDir, encodedRegionName);
  }

  /**
   * Get the path to the table archive directory based on the configured archive directory.
   * <p>
   * Assumed that the table should already be archived.
   * @param conf {@link Configuration} to read the archive directory property. Can be null
   * @param tabledir directory of the table to be archived. Cannot be null.
   * @return {@link Path} to the archive directory for the table
   */
  public static Path getTableArchivePath(Configuration conf, Path tabledir) {
    String archiveName = getConfiguredArchiveDirName(conf);
    Path root = tabledir.getParent();
    // now build the archive directory path
    // first the top-level archive directory
    // generally "/hbase/.archive/[table]
    return archiveName.length() == 0 ? new Path(root, tabledir) : new Path(new Path(root,
        archiveName), tabledir.getName());
  }

  /**
   * Get the archive directory as per the configuration
   * @param conf {@link Configuration} to read the archive directory from (can be null, in which
   *          case you get the default value). Can be null.
   * @return the configured archived directory or the default specified by
   *         {@value HFileArchiveUtil#DEFAULT_HFILE_ARCHIVE_DIRECTORY}
   */
  public static String getConfiguredArchiveDirName(Configuration conf) {
    return conf == null ? HFileArchiveUtil.DEFAULT_HFILE_ARCHIVE_DIRECTORY : conf.get(
      HConstants.HFILE_ARCHIVE_DIRECTORY, HFileArchiveUtil.DEFAULT_HFILE_ARCHIVE_DIRECTORY);
  }

  /**
   * Get the full path to the archive directory on the configured {@link FileSystem}
   * @param conf to look for archive directory name and root directory. Cannot be null. Notes for
   *          testing: requires a FileSystem root directory to be specified.
   * @return the full {@link Path} to the archive directory, as defined by the configuration
   * @throws IOException if an unexpected error occurs
   */
  public static Path getArchivePath(Configuration conf) throws IOException {
    return new Path(FSUtils.getRootDir(conf), getConfiguredArchiveDirName(conf));
  }
}
