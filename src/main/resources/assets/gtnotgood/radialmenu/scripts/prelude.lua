-- RadialMenu script prelude: loaded into a script's globals, in the same environment, just before the script itself.
--
-- Everything here could have been written in Java, and is not for one reason: these functions handle Lua values the host
-- has no business holding. A pattern that only string.match should interpret. An item table carrying the script
-- author's own fields and callbacks. So the contract across the Java line stays narrow - a chat line, a list of labels,
-- an index - and keys, patterns and callbacks live on this side of it.

local host_line = chat.line
local host_open = menu.open
local host_update = menu.update

-- Taken away once captured. Not a security boundary - the script's author wrote the script - but calling the host half
-- of menu.open by hand would skip every rule below it, and the symptom would be an entry's onPick never running.
chat.line = nil
menu.open = nil
menu.update = nil

local DEFAULT_WAIT = 60

--- Waits for a chat line matching a Lua pattern, and returns its captures.
--
-- The loop is here rather than in the host because the matching is: the host hands over lines as they arrive and says
-- how long each one took, and this decides whether it was the line being waited for.
function chat.await(pattern, timeoutTicks)
  if type(pattern) ~= "string" then
    error("chat.await expects a pattern string", 2)
  end

  local remaining = timeoutTicks or DEFAULT_WAIT
  while remaining > 0 do
    local line, waited = host_line(remaining)
    if line == nil then
      return nil
    end
    remaining = remaining - (waited or 0)

    local captures = { line:match(pattern) }
    if captures[1] ~= nil then
      return table.unpack(captures)
    end
  end
  return nil
end

--- Collects every line matching a pattern for a while, as a list of capture tables.
function chat.awaitAll(pattern, windowTicks)
  if type(pattern) ~= "string" then
    error("chat.awaitAll expects a pattern string", 2)
  end

  local remaining = windowTicks or DEFAULT_WAIT
  local found = {}
  while remaining > 0 do
    local line, waited = host_line(remaining)
    if line == nil then
      break
    end
    remaining = remaining - (waited or 0)

    local captures = { line:match(pattern) }
    if captures[1] ~= nil then
      found[#found + 1] = captures
    end
  end
  return found
end

-- Every entry the player is looking at, submenus included, in the order the host numbers them. A choice comes back as
-- one number, and with a tree of entries that number cannot be a position in any one list - so the numbering is kept
-- here, where the author's own tables are, and the host is told which number each entry it draws carries.
--
-- A choice resolves against this rather than against whatever the script has moved on to, because the player chose from
-- what was on screen.
local shown = nil

-- What a submenu entry may carry besides its own entries: how it opens, how many sectors, and the colours its ring is
-- drawn with. Copied by name rather than by handing the author's table over, for the same reason every other field here
-- is - what crosses the line is what the wheel draws, and nothing else.
local SUBMENU_OPTIONS = {
  "opening", "slots", "accent", "ring", "highlight", "border", "highlightBorder", "background",
}

--- Flattens the entries into `flat`, numbering each as the host will refer to it, and returns the list to draw.
--
-- Depth first and parents before children, so a number names the same entry on both sides of the line without either
-- having to describe the shape of the tree to the other.
local function normalize(items, level, flat, seen)
  local list = {}

  if #items > menu.maxEntries then
    log("menu was given " .. #items .. " entries and a wheel holds " .. menu.maxEntries
      .. "; the rest are not shown - page them with an entry of your own")
  end

  for i = 1, #items do
    local raw = items[i]
    local item
    if type(raw) == "table" then
      item = raw
    elseif type(raw) == "string" or type(raw) == "number" then
      -- A plain string is the common case: a name parsed out of a server's reply, which is its own key and its own
      -- label.
      item = { key = raw, label = tostring(raw) }
    else
      error("menu entry " .. i .. " is a " .. type(raw) .. ", expected a string or a table", level)
    end

    local key = item.key
    if key == nil then key = item.label end
    if key == nil then key = i end

    local label = item.label
    if label == nil then label = tostring(key) end

    if seen[key] ~= nil then
      log("menu has two entries keyed '" .. tostring(key) .. "'; a choice cannot say which of them it was")
    end
    seen[key] = true

    local entry = { item = item, key = key, label = label, icon = item.icon, color = item.color }
    flat[#flat + 1] = entry
    entry.n = #flat

    -- An entry carrying a list of its own is a submenu. Numbered like any other even though choosing it is the wheel's
    -- business rather than the script's: a submenu that skipped a number would make the numbering depend on which
    -- entries happen to have children, which is a thing for both sides to get wrong rather than one.
    if type(item.items) == "table" and #item.items > 0 then
      entry.children = normalize(item.items, level, flat, seen)
    end

    list[i] = entry
  end

  return list
end

-- What the host is allowed to see: what to draw, and nothing else. Extra fields, functions and the author's own tables
-- stay here.
local function project(list)
  local projected = {}
  for i = 1, #list do
    local entry = list[i]
    local out = { n = entry.n, label = entry.label, icon = entry.icon, color = entry.color }
    if entry.children ~= nil then
      out.items = project(entry.children)
      for j = 1, #SUBMENU_OPTIONS do
        local option = SUBMENU_OPTIONS[j]
        out[option] = entry.item[option]
      end
    end
    projected[i] = out
  end
  return projected
end

--- Opens a wheel and waits for a choice. Returns the chosen entry's key, and the entry itself.
--
-- An entry's onPick, if it has one, runs before this returns - so a callback that waits for chat or opens a menu of its
-- own is ordinary code - and its return value is ignored. The key is what comes back either way: what menu.open returns
-- must not depend on whether some entry happened to carry a callback.
function menu.open(items, opts)
  if type(items) ~= "table" then
    error("menu.open expects a table of entries", 2)
  end

  local flat = {}
  local list = normalize(items, 3, flat, {})
  if #list == 0 then
    return nil
  end

  shown = flat
  local index = host_open(project(list), opts)
  if index == nil then
    return nil
  end

  local chosen = shown[index]
  if chosen == nil then
    return nil
  end

  local item = chosen.item
  if type(item.onPick) == "function" then
    item.onPick(chosen.key, item)
  elseif item.onPick ~= nil then
    log("entry '" .. tostring(chosen.key) .. "' has an onPick that is a " .. type(item.onPick) .. ", not a function")
  end

  return chosen.key, item
end

--- Replaces the contents of a wheel left up by keepOpen, without waiting for a choice.
function menu.update(items)
  if type(items) ~= "table" then
    error("menu.update expects a table of entries", 2)
  end

  local flat = {}
  local list = normalize(items, 3, flat, {})
  shown = flat
  host_update(project(list))
end
