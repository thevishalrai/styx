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
package com.hotels.styx.api.extension.service.spi;

import com.hotels.styx.api.Environment;
import com.hotels.styx.api.Identifiable;
import com.hotels.styx.api.configuration.Configuration;
import com.hotels.styx.api.configuration.ServiceFactory;

import java.util.Collection;
import java.util.EventListener;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import static com.hotels.styx.api.extension.service.spi.Registry.Outcome.FAILED;
import static com.hotels.styx.api.extension.service.spi.Registry.Outcome.RELOADED;
import static com.hotels.styx.api.extension.service.spi.Registry.Outcome.UNCHANGED;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;
import static java.util.stream.StreamSupport.stream;

/**
 * Registry for resources of type {@code T}.
 *
 * @param <T> the type of resource to store
 */
public interface Registry<T extends Identifiable> extends Supplier<Iterable<T>> {

    /**
     * Register a {@link Registry.ChangeListener} to be notified when registry changes.
     *
     * @param changeListener {@link Registry.ChangeListener} to register.
     */
    Registry<T> addListener(ChangeListener<T> changeListener);

    /**
     * Remove a {@link Registry.ChangeListener}.
     *
     * @param changeListener {@link Registry.ChangeListener} to remove.
     */
    Registry<T> removeListener(ChangeListener<T> changeListener);

    CompletableFuture<ReloadResult> reload();

    /**
     * Factory for creating a registry.
     *
     * @param <T>
     */
    interface Factory<T extends Identifiable> extends ServiceFactory<Registry<T>> {
        Registry<T> create(Environment environment, Configuration registryConfiguration);
    }

    /**
     * A base interface for notification of a change in the registry.
     *
     * @param <T> The type of configuration referred to by this ChangeListener
     */
    interface ChangeListener<T extends Identifiable> extends EventListener {
        void onChange(Changes<T> changes);
    }

    /**
     * The set of changes between reloads.
     *
     * @param <T>
     */
    final class Changes<T extends Identifiable> {
        private final Collection<T> added;
        private final Collection<T> removed;
        private final Collection<T> updated;

        private Changes(Builder builder) {
            this.added = nullToEmpty(builder.added);
            this.removed = nullToEmpty(builder.removed);
            this.updated = nullToEmpty(builder.updated);
        }

        private static <T> Collection<T> nullToEmpty(Collection iterable) {
            return iterable != null ? iterable : emptyList();
        }

        public Iterable<T> added() {
            return added;
        }

        public Iterable<T> removed() {
            return removed;
        }

        public Iterable<T> updated() {
            return updated;
        }

        @Override
        public int hashCode() {
            return Objects.hash(added, removed, updated);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }
            Changes<T> other = (Changes<T>) obj;
            return equal(this.added, other.added)
                    && equal(this.removed, other.removed)
                    && equal(this.updated, other.updated);
        }

        private boolean equal(Collection<T> iterable1, Collection<T> iterable2) {
            return iterable1.size() == iterable2.size() && iterable1.containsAll(iterable2);
        }

        @Override
        public String toString() {
            return "Changes{"
                    + "added="
                    + added
                    + ", removed="
                    + removed
                    + ", updated="
                    + updated
                    + '}';
        }

        public boolean isEmpty() {
            return added.isEmpty() && removed.isEmpty() && updated.isEmpty();
        }

        public static class Builder<T extends Identifiable> {
            private Collection<T> added;
            private Collection<T> removed;
            private Collection<T> updated;

            public Builder<T> added(T... added) {
                return added(asList(added));
            }

            public Builder<T> added(Iterable<T> added) {
                this.added = collection(requireNonNull(added));
                return this;
            }

            public Builder<T> removed(T... added) {
                return removed(asList(added));
            }

            public Builder<T> removed(Iterable<T> removed) {
                this.removed = collection(requireNonNull(removed));
                return this;
            }

            public Builder<T> updated(T... updated) {
                return updated(asList(updated));
            }

            public Builder<T> updated(Iterable<T> updated) {
                this.updated = collection(requireNonNull(updated));
                return this;
            }

            private Collection<T> collection(Iterable<T> iterable) {
                // probably always true
                if (iterable instanceof Collection<?>) {
                    return (Collection<T>) iterable;
                }

                return stream(iterable.spliterator(), false).collect(toList());
            }

            public Changes<T> build() {
                return new Changes<>(this);
            }
        }
    }

    /**
     * Registry reload outcome.
     */
    enum Outcome {
        RELOADED,
        UNCHANGED,
        FAILED
    }

    /**
     * Result of registry reload.
     */
    class ReloadResult {
        private final Outcome outcome;
        private final String message;
        private final Throwable cause;

        private ReloadResult(Outcome outcome, String message, Throwable cause) {
            this.outcome = outcome;
            this.message = message;
            this.cause = cause;
        }

        public static ReloadResult reloaded(String message) {
            return new ReloadResult(RELOADED, message, null);
        }

        public static ReloadResult failed(String message, Throwable cause) {
            return new ReloadResult(FAILED, message, cause);
        }

        public static ReloadResult unchanged(String message) {
            return new ReloadResult(UNCHANGED, message, null);
        }

        public Outcome outcome() {
            return outcome;
        }

        public String message() {
            return message;
        }

        public Optional<Throwable> cause() {
            return Optional.ofNullable(cause);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }

            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            ReloadResult that = (ReloadResult) o;
            return outcome == that.outcome && Objects.equals(message, that.message) && Objects.equals(cause, that.cause);
        }

        @Override
        public int hashCode() {
            return Objects.hash(outcome, message, cause);
        }

        @Override
        public String toString() {
            return "ReloadResult{"
                    + "outcome="
                    + outcome
                    + ", message='" + message + '\''
                    + ", cause='" + cause + "\'"
                    + '}';
        }
    }
}
