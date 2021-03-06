/*
 * Copyright 2000-2011 JetBrains s.r.o.
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

package jetbrains.buildServer.tools.test;

import jetbrains.buildServer.tools.ScanFile;
import jetbrains.buildServer.tools.java.JavaVersion;
import jetbrains.buildServer.tools.rules.CheckHolder;
import jetbrains.buildServer.tools.rules.PathSettings;
import jetbrains.buildServer.tools.rules.StaticCheckRule;
import jetbrains.buildServer.tools.rules.VersionRule;
import org.jetbrains.annotations.Nullable;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.Collection;

import static jetbrains.buildServer.tools.java.JavaVersion.Java_1_7;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;

/**
 * @author Eugene Petrenko (eugene.petrenko@gmail.com)
 *         Date: 11.11.11 15:33
 */
public class RulesMatcherTest extends RulesBaseTestCase {
  @Test
  public void testMatchesFile() throws IOException {
    final PathSettings s = parseConfig("1.7 => aaa/bbb.jar\r\n-=>aaa/z");

    assertEquals(getVersionRule(s, mockFile("aaa/bbb.jar")), Java_1_7);
    assertEquals(getVersionRule(s, mockFile("aaa/bbb.jar!aaa")), Java_1_7);
    assertEquals(getVersionRule(s, mockFile("aaa/bbb.jar!aaa/bbb")), Java_1_7);
    assertEquals(getVersionRule(s, mockFile("aaa/bbb.jar!aaa/bbb.jar!zzz")), Java_1_7);
    assertEquals(getVersionRule(s, mockFile("aaa/zzz.jar")), null);
    assertEquals(getVersionRule(s, mockFile("aaa")), null);
  }

  @Test
  public void testExcludes() throws IOException {
    final PathSettings s = parseConfig("1.7 => aaa/bbb.jar\r\n-=>aaa/b");

    Assert.assertTrue(isPathExcluded(s, mockFile("aaa/b")));
    Assert.assertTrue(isPathExcluded(s, mockFile("aaa/bb")));
    Assert.assertTrue(isPathExcluded(s, mockFile("aaa/bbq")));

    Assert.assertTrue(isPathUnknown(s, mockFile("ppp/bbq")));

    //excludes wins
    Assert.assertTrue(isPathExcluded(s, mockFile("aaa/bbb.jar")));
    Assert.assertTrue(isPathExcluded(s, mockFile("aaa/bbb.jar!sss")));
  }

  private boolean isPathUnknown(PathSettings s, ScanFile f) {
    return s.getFileCheckMode(f) == null;
  }

  private boolean isPathExcluded(PathSettings s, ScanFile f) {
    final Collection<? extends CheckHolder> mode = s.getFileCheckMode(f);
    return mode != null && mode.isEmpty();
  }

  @Nullable
  private JavaVersion getVersionRule(PathSettings s, ScanFile f) {
    final Collection<? extends CheckHolder> mode = s.getFileCheckMode(f);
    if (mode == null) return null;
    for (CheckHolder holder : mode) {
      if (holder instanceof VersionRule) {
        return ((VersionRule) holder).getVersion();
      }
    }
    return null;
  }

  @Nullable
  private StaticCheckRule getStaticRule(PathSettings s, ScanFile f) {
    final Collection<? extends CheckHolder> mode = s.getFileCheckMode(f);
    if (mode == null) return null;

    for (CheckHolder holder : mode) {
      if (holder instanceof StaticCheckRule) {
        return ((StaticCheckRule) holder);
      }
    }
    return null;
  }

  @Test
  public void testLongestRuleWins() throws IOException {
    final PathSettings s = parseConfig("1.7 => aaa/bbb/ccc/ddd.jar\r\n1.2=>");

    assertEquals(getVersionRule(s, mockFile("aaa")), JavaVersion.Java_1_2);
    assertEquals(getVersionRule(s, mockFile("aaa/bbb.jar")), JavaVersion.Java_1_2);
    assertEquals(getVersionRule(s, mockFile("aaa/bbb.jar!aaa")), JavaVersion.Java_1_2);
    assertEquals(getVersionRule(s, mockFile("aaa/bbb/bbb.jar!aaa/bbb")), JavaVersion.Java_1_2);
    assertEquals(getVersionRule(s, mockFile("aaa/bbb/ccc/bbb.jar!aaa/bbb")), JavaVersion.Java_1_2);
    assertEquals(getVersionRule(s, mockFile("aaa/bbb/ccc/ddd/bbb.jar!aaa/bbb")), JavaVersion.Java_1_2);
    assertEquals(getVersionRule(s, mockFile("aaa/bbb.jar!aaa/bbb.jar!zzz")), JavaVersion.Java_1_2);

    assertEquals(getVersionRule(s, mockFile("aaa/bbb/ccc/ddd.jar!aaa/bbb.jar!zzz")), Java_1_7);
    assertEquals(getVersionRule(s, mockFile("aaa/bbb/ccc/ddd.jar")), Java_1_7);
    assertEquals(getVersionRule(s, mockFile("aaa/bbb/ccc/ddd.jarddd")), Java_1_7);
    assertEquals(getVersionRule(s, mockFile("aaa/bbb/ccc/ddd.jar!e/d/sd")), Java_1_7);
  }

  @Test
  public void testStaticRules() throws IOException {
    final PathSettings s = parseConfig("check static => aaa/bbb/ccc/ddd.jar\n - check static => aaa/bbb/ccc/ddd.jar!z");

    assertNull(getStaticRule(s, mockFile("aaa")));
    assertNull(getStaticRule(s, mockFile("aaa/bbb.jar")));
    assertNull(getStaticRule(s, mockFile("aaa/bbb.jar!aaa")));
    assertNull(getStaticRule(s, mockFile("aaa/bbb/bbb.jar!aaa/bbb")));
    assertNull(getStaticRule(s, mockFile("aaa/bbb/ccc/bbb.jar!aaa/bbb")));
    assertNull(getStaticRule(s, mockFile("aaa/bbb/ccc/ddd/bbb.jar!aaa/bbb")));
    assertNull(getStaticRule(s, mockFile("aaa/bbb.jar!aaa/bbb.jar!zzz")));

    assertNotNull(getStaticRule(s, mockFile("aaa/bbb/ccc/ddd.jar!aaa/bbb.jar!zzz")));
    assertNotNull(getStaticRule(s, mockFile("aaa/bbb/ccc/ddd.jar!aaa/bbb.jar!zzz.class")));
    assertNotNull(getStaticRule(s, mockFile("aaa/bbb/ccc/ddd.jar")));
    assertNotNull(getStaticRule(s, mockFile("aaa/bbb/ccc/ddd.jarddd")));
    assertNotNull(getStaticRule(s, mockFile("aaa/bbb/ccc/ddd.jar!e/d/sd")));

    assertNull(getStaticRule(s, mockFile("aaa/bbb/ccc/ddd.jar!zzz")));
  }

}
