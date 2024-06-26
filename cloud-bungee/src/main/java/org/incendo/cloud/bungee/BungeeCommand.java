//
// MIT License
//
// Copyright (c) 2024 Incendo
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files (the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions:
//
// The above copyright notice and this permission notice shall be included in all
// copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
// SOFTWARE.
//
package org.incendo.cloud.bungee;

import io.leangen.geantyref.GenericTypeReflector;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.TabExecutor;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.incendo.cloud.component.CommandComponent;
import org.incendo.cloud.internal.CommandNode;
import org.incendo.cloud.permission.Permission;
import org.incendo.cloud.suggestion.Suggestion;
import org.incendo.cloud.suggestion.Suggestions;
import org.incendo.cloud.util.StringUtils;

public final class BungeeCommand<C> extends Command implements TabExecutor {

    private final BungeeCommandManager<C> manager;
    private final CommandComponent<C> command;

    BungeeCommand(
            final org.incendo.cloud.@NonNull Command<C> cloudCommand,
            final @NonNull CommandComponent<C> command,
            final @NonNull BungeeCommandManager<C> manager
    ) {
        super(
                command.name(),
                cloudCommand.commandPermission().toString(),
                command.alternativeAliases().toArray(new String[0])
        );
        this.command = command;
        this.manager = manager;
    }

    @Override
    public void execute(final CommandSender commandSender, final String[] strings) {
        /* Join input */
        final StringBuilder builder = new StringBuilder(this.command.name());
        for (final String string : strings) {
            builder.append(" ").append(string);
        }
        final C sender = this.manager.senderMapper().map(commandSender);
        this.manager.commandExecutor().executeCommand(sender, builder.toString());
    }

    @Override
    public boolean hasPermission(final CommandSender sender) {
        final CommandNode<C> node = this.namedNode();
        if (node == null) {
            return false;
        }

        final Map<Type, Permission> accessMap =
            node.nodeMeta().getOrDefault(CommandNode.META_KEY_ACCESS, Collections.emptyMap());
        final C cloudSender = this.manager.senderMapper().map(sender);
        for (final Map.Entry<Type, Permission> entry : accessMap.entrySet()) {
            if (GenericTypeReflector.isSuperType(entry.getKey(), cloudSender.getClass())) {
                if (this.manager.testPermission(cloudSender, entry.getValue()).allowed()) {
                    return true;
                }
            }
        }
        return false;
    }

    private @Nullable CommandNode<C> namedNode() {
        return this.manager.commandTree().getNamedNode(this.command.name());
    }

    @Override
    public Iterable<String> onTabComplete(
            final CommandSender sender,
            final String[] args
    ) {
        final StringBuilder builder = new StringBuilder(this.command.name());
        for (final String string : args) {
            builder.append(" ").append(string);
        }
        final Suggestions<C, ?> result = this.manager.suggestionFactory().suggestImmediately(
                this.manager.senderMapper().map(sender),
                builder.toString()
        );
        return result.list().stream()
                .map(Suggestion::suggestion)
                .map(suggestion -> StringUtils.trimBeforeLastSpace(suggestion, result.commandInput()))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }
}
