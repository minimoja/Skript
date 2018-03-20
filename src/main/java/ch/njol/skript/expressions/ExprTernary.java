/**
 * This file is part of Skript.
 * <p>
 * Skript is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p>
 * Skript is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with Skript.  If not, see <http://www.gnu.org/licenses/>.
 * <p>
 * <p>
 * Copyright 2011-2017 Peter Güttinger and contributors
 */
package ch.njol.skript.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Condition;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.skript.util.LiteralUtil;
import ch.njol.skript.util.Utils;
import ch.njol.util.Kleenean;
import ch.njol.util.coll.CollectionUtils;
import org.bukkit.event.Event;
import org.eclipse.jdt.annotation.Nullable;

import java.lang.reflect.Array;

@Name("Ternary")
@Description("A shorthand expression for returning something based on a condition.")
@Examples({"broadcast "})
@Since("INSERT VERSION")
@SuppressWarnings("null")
public class ExprTernary<T> extends SimpleExpression<T> {

    static {
        Skript.registerExpression(ExprTernary.class, Object.class, ExpressionType.COMBINED,
                "%objects% if <.+>[,] (otherwise|else) %objects%");
    }

    private final ExprTernary<?> source;
    private final Class<T> superType;
    @Nullable
    private Expression<Object> ifTrue;
    @Nullable
    private Condition condition;
    @Nullable
    private Expression<Object> ifFalse;

    @SuppressWarnings("unchecked")
    public ExprTernary() {
        this(null, (Class<? extends T>) Object.class);
    }

    @SuppressWarnings("unchecked")
    private ExprTernary(ExprTernary<?> source, Class<? extends T>... types) {
        this.source = source;
        if (source != null) {
            this.ifTrue = source.ifTrue;
            this.ifFalse = source.ifFalse;
            this.condition = source.condition;
        }
        this.superType = (Class<T>) Utils.getSuperType(types);
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        ifTrue = LiteralUtil.defendExpression(exprs[0]);
        ifFalse = LiteralUtil.defendExpression(exprs[1]);
        if (ifFalse instanceof ExprTernary<?> || ifTrue instanceof ExprTernary<?>) {
            Skript.error("Ternary operators may not be nested!");
            return false;
        }
        String cond = parseResult.regexes.get(0).group();
        condition = Condition.parse(cond, "Can't understand this condition: " + cond);
        return condition != null && LiteralUtil.canInitSafely(ifTrue, ifFalse);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected T[] get(Event e) {
        Object[] values = condition.check(e) ? ifTrue.getArray(e) : ifFalse.getArray(e);
        try {
            return CollectionUtils.convertArray(values, superType);
        } catch (ClassCastException e1) {
            return (T[]) Array.newInstance(superType, 0);
        }
    }

    @Override
    public <R> Expression<? extends R> getConvertedExpression(Class<R>... to) {
        return new ExprTernary<>(this, to);
    }

    @Override
    public Expression<?> getSource() {
        return source == null ? this : source;
    }

    @Override
    public Class<? extends T> getReturnType() {
        return superType;
    }

    @Override
    public boolean isSingle() {
        return ifTrue.isSingle() && ifFalse.isSingle();
    }

    @Override
    public String toString(Event e, boolean debug) {
        return ifTrue.toString(e, debug) + " if " + ifFalse.toString(e, debug);
    }

}
