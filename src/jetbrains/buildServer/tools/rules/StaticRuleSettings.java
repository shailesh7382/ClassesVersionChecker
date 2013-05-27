package jetbrains.buildServer.tools.rules;

import org.jetbrains.annotations.NotNull;

import java.util.Set;
import java.util.TreeSet;

/**
 * Created 27.05.13 18:05
 *
 * @author Eugene Petrenko (eugene.petrenko@jetbrains.com)
 */
public class StaticRuleSettings {
  private final Set<String> myAllowedClasses = new TreeSet<String>();

  public boolean isClassAllowed(@NotNull String name) {
    return myAllowedClasses.contains(name);
  }

  public void addRule(@NotNull String rule) {
    myAllowedClasses.add(rule);
  }
}
