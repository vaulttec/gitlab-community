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

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;

@Configuration
public class ThymeleafHelperConfig {

  @Bean(name = "pageHelper")
  public PageHelper pageHelper() {

    return new PageHelper() {

      public String sortProperties(Page<?> page) {
        StringBuilder builder = new StringBuilder();
        if (page.getSort() != null) {
          for (Sort.Order order : page.getSort()) {
            builder.append(encodeURLComponent(order.getProperty()));
            builder.append(",");
            builder.append(encodeURLComponent(order.getDirection().name().toLowerCase()));
          }
        }
        return builder.toString();
      }

      public Integer[] pageButtonSequence(Page<?> page, Integer maxPageButtons) {
        int start = Math.max(0,
            (page.getTotalPages() - page.getNumber()) <= maxPageButtons / 2 ? page.getTotalPages() - maxPageButtons + 1
                : page.getNumber() - maxPageButtons / 2 + 1);
        int end = Math.min(page.getNumber() <= maxPageButtons / 2 ? maxPageButtons : start + maxPageButtons - 1,
            page.getTotalPages());
        List<Integer> buttonPages = new ArrayList<Integer>();
        for (int i = start; i <= end; i++) {
          buttonPages.add(Integer.valueOf(i));
        }
        return buttonPages.toArray(new Integer[buttonPages.size()]);
      }

      private String encodeURLComponent(String component) {
        try {
          return URLEncoder.encode(component, "UTF-8");
        } catch (UnsupportedEncodingException e) {
          throw new RuntimeException(e);
        }
      }
    };
  }

  public interface PageHelper {
    String sortProperties(Page<?> page);
    Integer[] pageButtonSequence(Page<?> page, Integer maxButtonPages);
  }
}
