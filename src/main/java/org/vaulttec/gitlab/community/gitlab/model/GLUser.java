/*
 * GitLab Community
 * Copyright (c) 2020 Torsten Juergeleit
 * mailto:torsten AT vaulttec DOT org
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
package org.vaulttec.gitlab.community.gitlab.model;

import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.util.StringUtils;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSetter;

@JsonIgnoreProperties(ignoreUnknown = true)
public class GLUser {
  public static final String CUSTOM_ATTRIBUTE_JOINED = "community_joined";
  public static final DateTimeFormatter JOINED_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;

  private String id;
  private String username;
  private String name;
  private String email;
  @JsonAlias("avatar_url")
  private URL avatar;
  @JsonAlias("web_url")
  private URL profile;
  private String bio;
  private Map<String, String> customAttributes = new HashMap<String, String>();

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public URL getAvatar() {
    return avatar;
  }

  public void setAvatar(URL avatar) {
    this.avatar = avatar;
  }

  public URL getProfile() {
    return profile;
  }

  public void setProfile(URL profile) {
    this.profile = profile;
  }

  public String getBio() {
    return bio;
  }

  public void setBio(String bio) {
    this.bio = bio;
  }

  public void addCustomAttribute(String key, String value) {
    this.customAttributes.put(key, value);
  }

  public boolean hasCustomAttribute(String key) {
    return customAttributes.containsKey(key);
  }

  public String getCustomAttribute(String key) {
    return customAttributes.get(key);
  }

  @JsonSetter("custom_attributes")
  public void setCustomAttributes(List<GLCustomAttribute> customAttributes) {
    this.customAttributes.clear();
    customAttributes.forEach(attribute -> this.customAttributes.put(attribute.getKey(), attribute.getValue()));
  }

  public LocalDate getJoined() {
    String joined = getCustomAttribute(CUSTOM_ATTRIBUTE_JOINED);
    return StringUtils.hasText(joined) ? LocalDate.parse(joined, JOINED_FORMATTER) : null;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((id == null) ? 0 : id.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    GLUser other = (GLUser) obj;
    if (id == null) {
      if (other.id != null) {
        return false;
      }
    } else if (!id.equals(other.id)) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    return "GLUser [id=" + id + ", username=" + username + ", name=" + name + ", email=" + email + ", avatar=" + avatar
        + ", profile=" + profile + ", bio=" + bio + ", customAttributes=" + customAttributes + "]";
  }
}
