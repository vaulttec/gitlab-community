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
package org.vaulttec.gitlab.community.web;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.security.oauth2.core.oidc.user.OidcUserAuthority;
import org.vaulttec.gitlab.community.CommunityPermission;
import org.vaulttec.gitlab.community.CommunityService;
import org.vaulttec.gitlab.community.model.Member;

import nz.net.ultraq.thymeleaf.LayoutDialect;

@Configuration
@EnableWebSecurity
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {

  private static final Logger LOG = LoggerFactory.getLogger(WebSecurityConfig.class);

  @Autowired
  private CommunityService communityService;

  @Bean
  public LayoutDialect layoutDialect() {
    return new LayoutDialect();
  }

  @Override
  public void configure(WebSecurity web) {
    web.ignoring().antMatchers("/webjars/**").antMatchers("/css/**").antMatchers("/img/**");
  }

  @Override
  protected void configure(HttpSecurity http) throws Exception {
    http.authorizeRequests().anyRequest().authenticated().and().oauth2Login().userInfoEndpoint()
        .userAuthoritiesMapper(this.userAuthoritiesMapper());
  }

  /**
   * Checks for all the OIDC user authorities the GitLab group membership (config
   * property "community.group-path") and the GitLab group permission (config
   * property "community.admin-permission").
   * <p>
   * For no members of the community GitLab group membership the guest role is
   * set. Community group members with admin group permission get the admin role.
   * All other community group members with admin group permission get the user
   * role.
   * <p>
   * All non OIDC user authorities are removed.
   */
  @SuppressWarnings("unchecked")
  private GrantedAuthoritiesMapper userAuthoritiesMapper() {
    return (authorities) -> {
      Set<GrantedAuthority> mappedAuthorities = new HashSet<>();
      for (GrantedAuthority authority : authorities) {
        if (OidcUserAuthority.class.isInstance(authority)) {
          OidcUserAuthority userAuthority = (OidcUserAuthority) authority;
          Map<String, Object> attributes = userAuthority.getAttributes();
          LOG.debug("Mapping authority '{}' {}", userAuthority, userAuthority.getAttributes());
          if (!attributes.containsKey("groups")
              || !((List<String>) attributes.get("groups")).contains(communityService.getCommunity().getPath())) {
            authority = new OidcUserAuthority(CommunityPermission.GUEST.getRole(), userAuthority.getIdToken(),
                userAuthority.getUserInfo());
            LOG.info("Mapped authority of '{}' ({}) to '{}'", attributes.get("nickname"), attributes.get("name"),
                authority);
          } else if (attributes.containsKey("nickname")) {
            Member member = communityService.getMember((String) attributes.get("nickname"));
            if (member != null && member.isAdmin()) {
              authority = new OidcUserAuthority(CommunityPermission.ADMIN.getRole(), userAuthority.getIdToken(),
                  userAuthority.getUserInfo());
              LOG.info("Mapped authority of '{}' ({}) to '{}'", attributes.get("nickname"), attributes.get("name"),
                  authority);
            }
          }
          mappedAuthorities.add(authority);
        }
      }
      return mappedAuthorities;
    };
  }
}
