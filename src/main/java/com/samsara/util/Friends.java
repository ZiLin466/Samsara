package com.samsara.util;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.TreeSet;

public final class Friends {
   private static final TreeSet<String> names = new TreeSet<>();
   private Friends() { }
   public static boolean contains(String name) { return names.contains(name.toLowerCase(Locale.ROOT)); }
   public static void add(String name) { names.add(name.toLowerCase(Locale.ROOT)); }
   public static void remove(String name) { names.remove(name.toLowerCase(Locale.ROOT)); }
   public static List<String> names() { return List.copyOf(names); }
   public static void restore(Collection<String> values) { names.clear(); values.forEach(Friends::add); }
}
