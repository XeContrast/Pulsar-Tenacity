package dev.tenacity.module.settings;

import dev.tenacity.module.settings.impl.BooleanSetting;
import net.minecraft.util.ChatComponentText;

import java.util.Arrays;
import java.util.function.Predicate;

import static dev.tenacity.utils.misc.MinecraftInstance.mc;

public class ParentAttribute<T extends Setting> {

    public final static Predicate<BooleanSetting> BOOLEAN_CONDITION = BooleanSetting::isEnabled;

    private final T parent;
    private final Predicate<T> condition;

    public ParentAttribute(T parent, Predicate<T> condition) {
        this.parent = parent;
        this.condition = condition;
    }

    public boolean isValid() {
        return condition.test(parent) && parent.getParents().stream().allMatch(ParentAttribute::isValid);
    }

    public T getParent() {
        return parent;
    }

}