package org.vaulttec.gitlab.community;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public enum CommunityPermission {
  GUEST("ROLE_GUEST"), USER("ROLE_USER"), ADMIN("ROLE_ADMIN");

  private String role;

  private CommunityPermission(String role) {
    this.role = role;
  }

  private final static Map<String, CommunityPermission> ENUM_NAME_MAP;
  static {
    ENUM_NAME_MAP = Arrays.stream(CommunityPermission.values())
        .collect(Collectors.toMap(CommunityPermission::getRole, Function.identity()));
  }

  public String getRole() {
    return role;
  }

  public static CommunityPermission fromRole(String role) {
    return ENUM_NAME_MAP.get(role);
  }
}
