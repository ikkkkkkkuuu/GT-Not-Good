package com.xyp.gtnotgood.client.radialmenu.core.action;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.xyp.gtnotgood.client.radialmenu.core.json.LowercaseEnumAdapterFactory;

public class ActionSpec {

    public String type;

    public Map<String, String> params;

    public List<ActionSpec> steps;

    public transient int cycleCursor;

    public ActionSpec() {}

    public ActionSpec(String type) {
        this.type = type;
    }

    public Map<String, String> paramsOrEmpty() {
        if (params == null) {
            params = new LinkedHashMap<>();
        }
        return params;
    }

    public List<ActionSpec> stepsOrEmpty() {
        if (steps == null) {
            steps = new ArrayList<>();
        }
        return steps;
    }

    public ActionSpec set(String key, String value) {
        paramsOrEmpty().put(key, value);
        return this;
    }

    public String getString(String key, String fallback) {
        if (params == null) {
            return fallback;
        }
        String value = params.get(key);
        return value == null ? fallback : value;
    }

    public int getInt(String key, int fallback) {
        try {
            String value = getString(key, null);
            return value == null ? fallback : Integer.parseInt(value.trim());
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    public boolean getBoolean(String key, boolean fallback) {
        String value = getString(key, null);
        return value == null ? fallback : Boolean.parseBoolean(value.trim());
    }

    public <E extends Enum<E>> E getEnum(String key, Class<E> enumType, E fallback) {
        String value = getString(key, null);
        if (value == null) {
            return fallback;
        }
        for (E constant : enumType.getEnumConstants()) {
            if (
                constant.name().equalsIgnoreCase(value.trim())
                    || LowercaseEnumAdapterFactory.externalName(constant).equalsIgnoreCase(value.trim())
            ) {
                return constant;
            }
        }
        return fallback;
    }

    public void normalize() {
        if (steps == null) {
            return;
        }
        Iterator<ActionSpec> iterator = steps.iterator();
        while (iterator.hasNext()) {
            ActionSpec step = iterator.next();
            if (step == null || step.type == null || step.type.trim().isEmpty()) {
                iterator.remove();
            } else {
                step.normalize();
            }
        }
        if (steps.isEmpty()) {
            steps = null;
        }
    }

    public ActionSpec copy() {
        ActionSpec copy = new ActionSpec(type);
        if (params != null) {
            copy.params = new LinkedHashMap<>(params);
        }
        if (steps != null) {
            copy.steps = new ArrayList<>(steps.size());
            for (ActionSpec step : steps) {
                copy.steps.add(step == null ? null : step.copy());
            }
        }
        return copy;
    }
}
