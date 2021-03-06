/*
  Copyright (C) 2013-2021 Expedia Inc.

  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
 */
package com.hotels.styx.api.extension.service;


import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.Integer.parseInt;
import static java.util.regex.Pattern.compile;

/**
 * Encapsulates configuration for a Styx URL path replacement rule.
 */
public class RewriteConfig implements RewriteRule {
    private final String urlPattern;
    private final String replacement;

    private final Pattern compiledUrlPattern;
    private final Replacement preprocessedReplacement;

    /**
     * Constructs an instance.
     *
     * @param urlPattern  URL pattern to match
     * @param replacement replacement for URL that can contain placeholders like $1, $2... for captured groups
     */
    public RewriteConfig(String urlPattern, String replacement) {
        this.urlPattern = urlPattern;
        this.replacement = replacement;

        this.compiledUrlPattern = compile(urlPattern);
        this.preprocessedReplacement = new Replacement(replacement);
    }

    public String urlPattern() {
        return urlPattern;
    }

    public String replacement() {
        return replacement;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        RewriteConfig that = (RewriteConfig) o;

        if (!replacement.equals(that.replacement)) {
            return false;
        }
        if (!urlPattern.equals(that.urlPattern)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int result = urlPattern.hashCode();
        result = 31 * result + replacement.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return new StringBuilder(64)
                .append(this.getClass().getSimpleName())
                .append("{urlPattern=")
                .append(urlPattern)
                .append(", replacement=")
                .append(replacement)
                .append('}')
                .toString();
    }

    @Override
    public Optional<String> rewrite(String originalUri) {
        Matcher matcher = compiledUrlPattern.matcher(originalUri);
        if (matcher.matches()) {
            return Optional.of(preprocessedReplacement.substitute(matcher));
        }

        return Optional.empty();
    }

    private static class Replacement {
        private static final String REGEX = "\\$\\d+";

        private final List<Integer> placeholderNumbers;
        private final List<String> literals;

        public Replacement(String replacement) {
            this.placeholderNumbers = placeholderNumbers(replacement);
            this.literals = literals(replacement);
        }

        private String substitute(MatchResult matcher) {
            StringBuilder rewrittenUrl = new StringBuilder();

            // There may not be enough literals to fully interleave with placeholders.
            // This is just how String.split(REGEX) works.
            // Any remaining literals are assumed to be empty strings.
            for (int i = 0; i < placeholderNumbers.size(); i++) {
                if (literals.size() > i) {
                    rewrittenUrl.append(literals.get(i));
                }
                rewrittenUrl.append(matcher.group(placeholderNumbers.get(i)));
            }

            if (literals.size() > placeholderNumbers.size()) {
                rewrittenUrl.append(literals.get(literals.size() - 1));
            }

            return rewrittenUrl.toString();
        }

        private static List<String> literals(String replacement) {
            return List.of(replacement.split(REGEX));
        }

        private static List<Integer> placeholderNumbers(String replacement) {
            Pattern pattern = compile(REGEX);

            Matcher matcher = pattern.matcher(replacement);
            List<Integer> placeholderNumbers = new ArrayList<>();
            while (matcher.find()) {
                String nextGroup = matcher.group();
                placeholderNumbers.add(placeholderNumber(nextGroup));
            }

            return List.copyOf(placeholderNumbers);
        }

        private static int placeholderNumber(String nextGroup) {
            return parseInt(removeDollar(nextGroup));
        }

        private static String removeDollar(String group) {
            return group.substring(1);
        }
    }
}



