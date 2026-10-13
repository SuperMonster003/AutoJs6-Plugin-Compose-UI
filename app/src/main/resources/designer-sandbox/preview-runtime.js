// 3-Block Compose designer preview runtime (roadmap P9.3 step 2, D43).
//
// Runs inside the designer's Rhino sandbox before the user's script. It provides a preview implementation of
// the `compose` script API with the host's visible semantics (state, render, node handles, modifier chains,
// memo, batch, refs, post and timers), but nothing reaches AutoJs6: the tree is handed to the designer canvas
// as JSON, events come back from the canvas, and other AutoJs6 modules throw "not available in the preview".
// The Kotlin side (SandboxEngine) owns the interpreter and its limits. The bridge is strings only: the catalog
// arrives as the JSON text `__catalogJson`, and Kotlin reads snapshots and console lines through `__sandbox`,
// so no Java object is ever visible to the script.
(function (global, catalogJson) {
    'use strict';

    var catalog = JSON.parse(catalogJson);
    var CALLBACK_BASE = 2000000;
    var MAX_NODE_ID = 999999;
    var MAX_NODES = 5000;
    var MAX_DEPTH = 64;
    var MAX_TIMERS = 64;
    var MAX_LINES = 500;
    var MAX_LINE_CHARS = 4000;

    // ------------------------------------------------------------------ errors

    function ComposeError(code, message, prop) {
        var error = new Error(message || code);
        error.name = 'ComposeError';
        error.code = code;
        if (prop !== undefined) error.prop = prop;
        Object.setPrototypeOf(error, ComposeError.prototype);
        return error;
    }
    ComposeError.prototype = Object.create(Error.prototype);
    ComposeError.prototype.constructor = ComposeError;
    ComposeError.prototype.name = 'ComposeError';

    function fail(code, message, prop) { throw new ComposeError(code, message, prop); }

    // ------------------------------------------------------------------ console and host modules

    function text(values) {
        var parts = [];
        for (var i = 0; i < values.length; i++) {
            var value = values[i];
            if (typeof value === 'string') parts.push(value);
            else {
                try { parts.push(JSON.stringify(value) === undefined ? String(value) : JSON.stringify(value)); }
                catch (ignored) { parts.push(String(value)); }
            }
        }
        return parts.join(' ');
    }
    var lines = [];
    var dropped = 0;
    function emit(level, message) {
        if (lines.length >= MAX_LINES) { dropped++; return; }
        lines.push({ level: level, text: message.length > MAX_LINE_CHARS ? message.substring(0, MAX_LINE_CHARS) + '...' : message });
    }
    function describe(error) {
        if (error instanceof Error) return (error.name || 'Error') + ': ' + error.message + (error.lineNumber ? ' (line ' + error.lineNumber + ')' : '');
        return 'Uncaught ' + text([error]);
    }
    function logger(level) { return function () { emit(level, text(arguments)); }; }
    var consoleObject = {
        log: logger('log'), info: logger('info'), warn: logger('warn'), error: logger('error'),
        verbose: logger('log'), debug: logger('log'), print: logger('log'),
    };
    global.console = consoleObject;
    global.log = logger('log');
    global.print = logger('log');
    global.toast = function (message) { emit('toast', text([message])); };
    global.toastLog = function (message) { emit('toast', text([message])); emit('log', text([message])); };

    var UNAVAILABLE = [
        'app', 'auto', 'automator', 'base64', 'colors', 'crypto', 'device', 'dialogs', 'engines', 'events', 'files', 'floaty',
        'http', 'images', 'keys', 'media', 'notice', 'ocr', 'opencc', 'plugins', 'power', 'recorder', 'sensors', 'shell',
        'storages', 'threads', 'timers', 'ui', 'util', 'web', 'zip', 'pickup', 'selector', 'click', 'longClick', 'press',
        'swipe', 'gesture', 'gestures', 'launch', 'launchApp', 'launchPackage', 'sleep', 'exit', 'currentPackage',
        'currentActivity', 'waitForActivity', 'waitForPackage', 'require', 'importClass', 'importPackage', 'runtime',
        'context', 'activity', 'java', 'javax', 'android', 'Packages', 'JavaImporter', 'org', 'com', 'net',
    ];
    // Plain writable globals, as in the host, so a script may still declare its own `colors` or `ui`; using one
    // (reading a member, calling it, constructing it) throws. Their values are proxies, never Java objects.
    UNAVAILABLE.forEach(function (name) {
        function unavailable() { throw new Error(name + ' is not available in the designer preview'); }
        global[name] = new Proxy(function () {}, {
            get: unavailable, set: unavailable, has: unavailable, apply: unavailable, construct: unavailable,
            getPrototypeOf: unavailable, ownKeys: unavailable, getOwnPropertyDescriptor: unavailable,
        });
    });

    // ------------------------------------------------------------------ timers

    var timers = [];
    var nextTimerId = 1;
    function addTimer(fn, delay, args, repeat) {
        if (typeof fn !== 'function') fail('INVALID_ARGUMENT', 'Expected a function');
        if (timers.length >= MAX_TIMERS) fail('LIMIT_EXCEEDED', 'Too many pending timers in the preview');
        var wait = Math.max(0, Number(delay) || 0);
        var timer = { id: nextTimerId++, at: Date.now() + wait, every: repeat ? Math.max(16, wait) : 0, fn: fn, args: args };
        timers.push(timer);
        return timer.id;
    }
    function removeTimer(id) { timers = timers.filter(function (timer) { return timer.id !== id; }); }
    global.setTimeout = function (fn, delay) { return addTimer(fn, delay, Array.prototype.slice.call(arguments, 2), false); };
    global.setInterval = function (fn, delay) { return addTimer(fn, delay, Array.prototype.slice.call(arguments, 2), true); };
    global.clearTimeout = removeTimer;
    global.clearInterval = removeTimer;

    // ------------------------------------------------------------------ state

    var dirty = false;
    var changed = false;
    var rendering = false;

    function same(a, b) {
        if (typeof a === 'number' && typeof b === 'number') return a === b ? (a !== 0 || 1 / a === 1 / b) : (a !== a && b !== b);
        return a === b;
    }

    function State(initial) {
        var stored = initial;
        Object.defineProperty(this, 'value', {
            enumerable: true,
            get: function () { return stored; },
            set: function (next) { if (!same(stored, next)) { stored = next; dirty = true; } },
        });
        Object.preventExtensions(this);
    }
    State.prototype.toString = function () { return '[object ComposeState]'; };

    function Ref() {
        var holder = this;
        holder.__node = null;
        Object.defineProperty(this, 'current', { enumerable: true, get: function () { return holder.__node; } });
    }

    // ------------------------------------------------------------------ modifier chains

    function Modifier(operations) {
        this.__operations = operations;
        Object.freeze(this);
    }
    Object.keys(catalog.modifiers).forEach(function (name) {
        var spec = catalog.modifiers[name];
        Modifier.prototype[name] = function () {
            var args = Array.prototype.slice.call(arguments);
            var signature = spec.signatures.filter(function (it) { return args.length >= it.required && args.length <= it.args.length; })[0];
            if (!signature) fail('INVALID_ARGUMENT', 'Modifier.' + name + ': Unsupported argument count', name);
            args.forEach(function (arg, index) {
                if (signature.args[index].callback && typeof arg !== 'function') fail('INVALID_ARGUMENT', 'Modifier.' + name + ': Expected a callback function', name);
            });
            if (this.__operations.length >= catalog.maxModifierOps) fail('LIMIT_EXCEEDED', 'Modifier chain exceeds contract limit', name);
            return new Modifier(this.__operations.concat([{ name: name, args: args, callbacks: signature.args.map(function (it) { return it.callback; }) }]));
        };
    });

    // ------------------------------------------------------------------ nodes

    var nextNodeId = 1;

    /** Node ids are given when a node is first drawn (or taken over by reconciliation), so drafts use none. */
    function identify(node) {
        if (node.__id === 0) {
            if (nextNodeId > MAX_NODE_ID) fail('LIMIT_EXCEEDED', 'The preview created too many nodes; leave the preview and enter it again');
            node.__id = nextNodeId++;
        }
        return node.__id;
    }

    function isNode(value) { return value instanceof Node; }
    function isNullish(value) { return value === null || value === undefined; }
    function isPlainObject(value) {
        return value !== null && typeof value === 'object' && !Array.isArray(value) && !isNode(value) && !(value instanceof Modifier) && !(value instanceof State);
    }
    function callbackProperty(event) { return 'on' + event.charAt(0).toUpperCase() + event.substring(1); }
    function componentSpec(type) {
        var spec = catalog.components[type];
        if (!spec) fail('UNKNOWN_COMPONENT', type + ': Unknown component');
        return spec;
    }
    function eventName(spec, type, name) {
        for (var i = 0; i < spec.events.length; i++) {
            if (spec.events[i] === name || callbackProperty(spec.events[i]) === name) return spec.events[i];
        }
        fail('UNKNOWN_PROP', type + '.' + name + ': Unknown event', name);
    }

    function Node(type, key) {
        var spec = componentSpec(type);
        if (!spec.node) fail('INVALID_ARGUMENT', type + ' is not a node component');
        var node = this;
        node.__id = 0;
        node.__type = type;
        node.__key = key;
        node.__spec = spec;
        node.__props = {};
        node.__modifier = null;
        node.__children = [];
        node.__slots = {};
        node.__listeners = {};
        node.__ref = null;
        node.__parent = null;
        node.__render = rendering;
        Object.defineProperty(node, 'type', { enumerable: true, value: type });
        Object.defineProperty(node, 'key', { enumerable: true, value: key });
        Object.defineProperty(node, 'nodeId', { enumerable: true, get: function () { return snapshotNodes[node.__id] === node ? node.__id : -1; } });
        Object.defineProperty(node, 'parent', { enumerable: true, get: function () { return node.__parent; } });
        Object.defineProperty(node, 'children', { enumerable: true, get: function () { return node.__children.slice(); } });
        spec.properties.concat(catalog.shortcuts).concat(['modifier']).forEach(function (name) {
            if (Object.prototype.hasOwnProperty.call(node, name) || name in Node.prototype) return;
            Object.defineProperty(node, name, {
                enumerable: true,
                get: function () { return node.get(name); },
                set: function (value) { var change = {}; change[name] = value; node.set(change); },
            });
        });
    }

    function touch() { changed = true; }

    Node.prototype.toString = function () { return 'ComposeNode(type=' + this.__type + ', nodeId=' + this.nodeId + ')'; };
    Node.prototype.get = function (name) {
        if (name === 'modifier') return this.__modifier === null ? undefined : this.__modifier;
        var canonical = this.__spec.canonical[name] || name;
        if (!this.__spec.canonical[name] && catalog.shortcuts.indexOf(name) < 0) fail('UNKNOWN_PROP', this.__type + '.' + name + ': Unknown property', name);
        if (canonical === 'text' && this.__spec.field && editors[this.__id]) return editors[this.__id].text;
        var value = this.__props[canonical];
        return value === undefined ? this.__spec.defaults[canonical] : value;
    };
    Node.prototype.set = function (props) { applyProperties(this, objectProperties(props, this.__type), false); touch(); return this; };
    Node.prototype.append = function (child) { return this.insert(this.__children.length, child); };
    Node.prototype.insert = function (index, child) {
        var additions = children(child);
        var next = this.__children.slice();
        var at = index;
        additions.forEach(function (node) {
            var before = next.indexOf(node);
            if (before >= 0) { next.splice(before, 1); if (before < at) at--; }
            next.splice(at++, 0, node);
        });
        replaceChildren(this, next);
        touch();
        return this;
    };
    Node.prototype.remove = function (child) {
        var target = typeof child === 'number' ? this.__children[child] : child;
        if (this.__children.indexOf(target) < 0) fail('NODE_DETACHED', this.__type + ': Not a child');
        replaceChildren(this, this.__children.filter(function (it) { return it !== target; }));
        touch();
        return this;
    };
    Node.prototype.replace = function (list) { replaceChildren(this, children(list)); touch(); return this; };
    Node.prototype.clear = function () { replaceChildren(this, []); touch(); return this; };
    Node.prototype.slot = function (name, child) { setSlot(this, name, singleChild(child)); touch(); return this; };
    Node.prototype.on = function (name, fn) {
        if (typeof fn !== 'function') fail('INVALID_ARGUMENT', 'Expected a function');
        var event = eventName(this.__spec, this.__type, name);
        this.__listeners[event] = (this.__listeners[event] || []).concat([fn]);
        touch();
        return this;
    };
    Node.prototype.off = function (name, fn) {
        var event = eventName(this.__spec, this.__type, name);
        this.__listeners[event] = isNullish(fn) ? [] : (this.__listeners[event] || []).filter(function (it) { return it !== fn; });
        touch();
        return this;
    };
    Node.prototype.focus = function () { return command(this, { kind: 'focus' }); };
    Node.prototype.blur = function () { return command(this, { kind: 'blur' }); };
    Node.prototype.scrollTo = function (target) {
        if (typeof target === 'number') return command(this, { kind: 'scrollTo', index: target });
        var options = objectProperties(target, this.__type);
        return command(this, { kind: 'scrollTo', index: options.index, offset: options.offset });
    };
    Node.prototype.edit = function (options) {
        if (!this.__spec.field) fail('INVALID_ARGUMENT', this.__type + ' is not a text field');
        identify(this);
        var values = objectProperties(options, this.__type);
        if (typeof values.text === 'string') {
            editor(this).text = values.text;
            editor(this).declared = values.text;
            this.__props.text = values.text;
        }
        touch();
        return this;
    };

    function command(node, request) {
        // As in the host: a command of the component, or of a modifier in its chain (focusable and the like).
        var supported = node.__spec.commands.indexOf(request.kind) >= 0 || (node.__modifier !== null && node.__modifier.__operations.some(function (operation) {
            return catalog.modifiers[operation.name].commands.indexOf(request.kind) >= 0;
        }));
        if (!supported) fail('INVALID_ARGUMENT', node.__type + ' does not support ' + request.kind);
        if (request.kind === 'scrollTo' && request.index !== undefined && (catalog.indexed.indexOf(node.__type) < 0 || request.index >= node.__children.length)) {
            fail('INVALID_ARGUMENT', node.__type + '.scrollTo: Invalid index');
        }
        request.id = identify(node);
        commands.push(request);
        // Commands travel with a tree, so a handler that only scrolls or focuses still publishes one.
        touch();
        return node;
    }

    function objectProperties(value, component) {
        if (isNullish(value)) return {};
        if (!isPlainObject(value)) fail('INVALID_ARGUMENT', component + '.props: Expected a property object', 'props');
        return value;
    }

    function applyProperties(node, raw, creating) {
        var spec = node.__spec;
        Object.keys(raw).forEach(function (name) {
            var value = raw[name];
            if (name === 'key' || name === 'children') {
                if (!creating) fail('UNKNOWN_PROP', node.__type + '.' + name + ': Unknown property', name);
                return;
            }
            if (name === 'ref') {
                if (!isNullish(value) && !(value instanceof Ref)) fail('INVALID_ARGUMENT', node.__type + '.ref: Expected compose.ref()', name);
                node.__ref = isNullish(value) ? null : value;
            } else if (name === 'modifier') {
                if (!isNullish(value) && !(value instanceof Modifier)) fail('INVALID_MODIFIER', node.__type + '.modifier: Expected compose.modifier()', name);
                node.__modifier = isNullish(value) ? null : value;
            } else if (name === 'slots') {
                var slots = objectProperties(value, node.__type);
                Object.keys(slots).forEach(function (slot) { setSlot(node, slot, singleChild(slots[slot])); });
            } else if (spec.slots.indexOf(name) >= 0) {
                setSlot(node, name, singleChild(value));
            } else if (spec.callbacks[name]) {
                if (!isNullish(value) && typeof value !== 'function') fail('INVALID_ARGUMENT', node.__type + '.' + name + ': Expected a function', name);
                node.__listeners[spec.callbacks[name]] = isNullish(value) ? [] : [value];
            } else if (name === 'type' || name === 'nodeId' || name === 'parent') {
                fail('UNKNOWN_PROP', node.__type + '.' + name + ': Unknown property', name);
            } else {
                var canonical = spec.canonical[name] || (catalog.shortcuts.indexOf(name) >= 0 ? name : null);
                if (canonical === null) fail('UNKNOWN_PROP', node.__type + '.' + name + ': Unknown property', name);
                if (isNullish(value)) delete node.__props[canonical];
                else node.__props[canonical] = value;
                if (canonical === 'text' && spec.field && editors[node.__id]) {
                    editors[node.__id].text = String(value);
                    editors[node.__id].declared = String(value);
                }
            }
        });
    }

    function replaceChildren(parent, list) {
        list.forEach(function (child) { adopt(parent, child); });
        parent.__children.forEach(function (child) { if (list.indexOf(child) < 0) child.__parent = null; });
        parent.__children = list.slice();
        list.forEach(function (child) { child.__parent = parent; });
    }

    function setSlot(parent, name, child) {
        if (parent.__spec.slots.indexOf(name) < 0) fail('UNKNOWN_PROP', parent.__type + '.' + name + ': Unknown slot', name);
        var old = parent.__slots[name];
        if (old && old !== child) old.__parent = null;
        if (child) { adopt(parent, child); parent.__slots[name] = child; child.__parent = parent; }
        else delete parent.__slots[name];
    }

    function adopt(parent, child) {
        for (var ancestor = parent; ancestor; ancestor = ancestor.__parent) {
            if (ancestor === child) fail('INVALID_ARGUMENT', parent.__type + ': A node cannot contain itself');
        }
        var old = child.__parent;
        if (old && old !== parent) {
            old.__children = old.__children.filter(function (it) { return it !== child; });
            Object.keys(old.__slots).forEach(function (slot) { if (old.__slots[slot] === child) delete old.__slots[slot]; });
        }
    }

    function children(raw) {
        var output = [];
        (function visit(value, depth) {
            if (depth > 64) fail('LIMIT_EXCEEDED', 'Children are nested too deeply');
            if (isNullish(value) || value === false) return;
            if (isNode(value)) output.push(value);
            else if (typeof value === 'string') output.push(create('Text', { text: value }));
            else if (Array.isArray(value)) value.forEach(function (item) { visit(item, depth + 1); });
            else fail('INVALID_ARGUMENT', 'A child must be a node, a string or an array');
        })(raw, 0);
        return output;
    }

    function singleChild(raw) {
        var list = children(raw);
        if (list.length > 1) fail('INVALID_ARGUMENT', 'A slot takes one node');
        return list.length ? list[0] : null;
    }

    function create(type, props, childInput) {
        var shorthand = typeof props === 'string' || isNode(props) || Array.isArray(props);
        if (shorthand && !isNullish(childInput)) fail('INVALID_ARGUMENT', type + ': Unexpected children');
        var raw = objectProperties(shorthand ? null : props, type);
        var input = shorthand ? props : childInput;
        if (Object.prototype.hasOwnProperty.call(raw, 'children') && !isNullish(input)) fail('INVALID_ARGUMENT', type + '.children: Given twice', 'children');
        var key = raw.key;
        if (!isNullish(key)) {
            if (typeof key === 'number' && isFinite(key)) key = String(key);
            else if (typeof key !== 'string') fail('INVALID_ARGUMENT', type + '.key: Expected a string or a number', 'key');
        } else key = null;
        var node = new Node(type, key);
        applyProperties(node, raw, true);
        var supplied = Object.prototype.hasOwnProperty.call(raw, 'children') ? raw.children : input;
        if (!isNullish(supplied)) replaceChildren(node, children(supplied));
        return node;
    }

    function factory(type, args) {
        var first = args[0];
        var isProps = !isNode(first) && (isNullish(first) || isPlainObject(first));
        var rest = Array.prototype.slice.call(args, isProps ? 1 : 0);
        return create(type, isProps ? first : null, rest.length ? rest : null);
    }

    // ------------------------------------------------------------------ mount and render

    var root = null;
    var renderFunction = null;
    var session = null;
    var mountKind = null;
    var commands = [];
    var editors = {};
    var snapshotNodes = {};
    var callbacks = {};

    function editor(node) {
        if (!editors[node.__id]) {
            var declared = isNullish(node.__props.text) ? '' : String(node.__props.text);
            editors[node.__id] = { text: declared, start: declared.length, end: declared.length, seq: 0, declared: declared };
        }
        return editors[node.__id];
    }

    function Session(kind) {
        var closed = false;
        var listeners = [];
        this.__close = function () {
            if (closed) return;
            closed = true;
            root = null; renderFunction = null; touch();
            listeners.forEach(function (fn) { fn.call(session); });
        };
        Object.defineProperty(this, 'root', { enumerable: true, get: function () { return closed ? null : root; } });
        this.isClosed = function () { return closed; };
        this.close = this.__close;
        this.update = function (fn) { return compose.batch(fn); };
        this.post = function (fn, delay) { compose.post(fn, delay); };
        this.on = function (event, fn) {
            if (event !== 'close') fail('INVALID_ARGUMENT', 'Session event must be close');
            listeners.push(fn);
            return this;
        };
        this.showSnackbar = function (message) { emit('snackbar', String(message)); return this; };
        if (kind === 'floaty') {
            var self = this;
            ['setPosition', 'setSize', 'requestFocus', 'disableFocus', 'setTouchable'].forEach(function (name) { self[name] = function () { return self; }; });
            ['getX', 'getY', 'getWidth', 'getHeight'].forEach(function (name) { self[name] = function () { return 0; }; });
        }
    }

    function isDraft(node) { return node.__render && node.__id === 0; }

    /**
     * Gives the drafts of a render the identities of the nodes they replace, like the host: by key, or by type
     * and ordinal among unkeyed siblings. A node that was not created by this render keeps its own identity,
     * and so does a node it might otherwise have matched, which is why such nodes are reserved first.
     */
    function reconcile(previous, draft) {
        var reserved = {};
        var used = {};
        (function collect(node) {
            if (!isDraft(node)) { if (node.__id !== 0) reserved[node.__id] = true; return; }
            node.__children.forEach(collect);
            Object.keys(node.__slots).forEach(function (name) { collect(node.__slots[name]); });
        })(draft);
        (function visit(candidate, input) {
            if (!isDraft(input)) return;
            var target = candidate && candidate.__id !== 0 && !reserved[candidate.__id] && !used[candidate.__id] &&
                candidate.__type === input.__type && candidate.__key === input.__key ? candidate : null;
            if (target !== null) { input.__id = target.__id; used[target.__id] = true; }
            var keyed = {};
            var unkeyed = {};
            if (target !== null) target.__children.forEach(function (child) {
                if (child.__key !== null) keyed[child.__key] = child;
                else (unkeyed[child.__type] = unkeyed[child.__type] || []).push(child);
            });
            var ordinals = {};
            input.__children.forEach(function (child) {
                var match;
                if (child.__key !== null) match = keyed[child.__key];
                else {
                    var index = ordinals[child.__type] || 0;
                    ordinals[child.__type] = index + 1;
                    match = (unkeyed[child.__type] || [])[index];
                }
                visit(match || null, child);
            });
            Object.keys(input.__slots).forEach(function (name) { visit(target === null ? null : target.__slots[name] || null, input.__slots[name]); });
        })(previous, draft);
    }

    function render() {
        if (!renderFunction) return;
        rendering = true;
        var result;
        try { result = renderFunction.call(undefined); } finally { rendering = false; }
        var next = singleChild(result);
        if (!next) fail('INVALID_ARGUMENT', 'render must return one ComposeNode');
        reconcile(root, next);
        if (next.__parent) adopt(null, next);
        next.__parent = null;
        root = next;
        touch();
    }

    function mount(kind, args) {
        if (rendering) fail('INVALID_ARGUMENT', 'Cannot create a Compose mount during rendering');
        var source = args[0];
        if (typeof source === 'function') {
            renderFunction = source;
            mountKind = kind;
            session = new Session(kind);
            render();
        } else {
            var node = singleChild(source);
            if (!node) fail('INVALID_ARGUMENT', 'mount requires a node or render function');
            renderFunction = null;
            root = node;
            mountKind = kind;
            session = new Session(kind);
            touch();
        }
        return session;
    }

    var compose = {
        isAvailable: function () { return true; },
        createElement: function (type) {
            if (typeof type !== 'string') fail('INVALID_ARGUMENT', 'createElement requires a component name');
            return factory(type.replace(/^compose:/, ''), Array.prototype.slice.call(arguments, 1));
        },
        modifier: function () { return new Modifier([]); },
        state: function (initial) { return new State(initial); },
        ref: function () { return new Ref(); },
        batch: function (fn) {
            if (typeof fn !== 'function') fail('INVALID_ARGUMENT', 'batch requires a function');
            return fn();
        },
        memo: function (fn, deps) {
            if (!rendering) fail('INVALID_ARGUMENT', 'memo can only be called while a render function runs');
            if (typeof fn !== 'function') fail('INVALID_ARGUMENT', 'memo requires a function');
            var node = singleChild(fn());
            if (!node) fail('INVALID_ARGUMENT', 'memo must return one ComposeNode');
            return node;
        },
        mount: function (source, options) { return mount('mount', arguments); },
        floaty: function (source, options) { return mount('floaty', arguments); },
        dialog: function (source, options) { return mount('dialog', arguments); },
        attach: function () { throw new Error('compose.attach needs an AutoJs6 UI page and is not available in the designer preview'); },
        post: function (fn, delay) { addTimer(fn, delay, [], false); },
        theme: function (spec) { if (arguments.length) themeInput = spec; return themeInput; },
        ComposeError: ComposeError,
    };
    var themeInput = {};
    Object.defineProperty(compose, 'version', { enumerable: true, get: function () { return JSON.parse(JSON.stringify(catalog.version)); } });
    Object.defineProperty(compose, 'sessions', { enumerable: true, get: function () { return session && !session.isClosed() ? [session] : []; } });
    Object.keys(catalog.components).forEach(function (type) {
        if (!catalog.components[type].node) return;
        compose[type] = function () { return factory(type, arguments); };
    });
    Object.freeze(compose);
    global.compose = compose;
    global.$compose = compose;
    global.ComposeError = ComposeError;

    // ------------------------------------------------------------------ snapshot for the canvas

    function plain(value, depth) {
        if (depth > 16) return { $expr: '[nested]' };
        if (value === null || typeof value === 'string' || typeof value === 'boolean') return value;
        if (typeof value === 'number') return isFinite(value) ? value : { $expr: String(value) };
        if (typeof value === 'function') return { $expr: '[function]' };
        if (Array.isArray(value)) return value.map(function (item) { return plain(item, depth + 1); });
        if (value instanceof State || value instanceof Ref || value instanceof Modifier || isNode(value)) return { $expr: String(value) };
        if (typeof value === 'object') {
            var out = {};
            Object.keys(value).forEach(function (name) { if (value[name] !== undefined) out[name] = plain(value[name], depth + 1); });
            return out;
        }
        return { $expr: String(value) };
    }

    function snapshot() {
        var nodes = {};
        var tokens = {};
        var count = 0;
        var nextToken = CALLBACK_BASE;
        var refs = [];
        var fields = {};
        var depth = 0;
        function visit(node) {
            if (++depth > MAX_DEPTH) fail('LIMIT_EXCEEDED', 'The preview tree is nested more than ' + MAX_DEPTH + ' levels deep');
            try { return describeNode(node); } finally { depth--; }
        }
        function describeNode(node) {
            identify(node);
            if (nodes[node.__id]) fail('INVALID_ARGUMENT', node.__type + ': A node appears twice in the tree');
            if (++count > MAX_NODES) fail('LIMIT_EXCEEDED', 'The preview tree has more than ' + MAX_NODES + ' nodes');
            nodes[node.__id] = node;
            if (node.__ref) refs.push([node.__ref, node]);
            var props = {};
            Object.keys(node.__props).forEach(function (name) { props[name] = plain(node.__props[name], 0); });
            if (node.__spec.field) {
                var current = editor(node);
                var declared = isNullish(node.__props.text) ? '' : String(node.__props.text);
                if (declared !== current.declared) { current.text = declared; current.declared = declared; }
                props.text = current.text;
                fields[node.__id] = current.seq;
            }
            var events = Object.keys(node.__listeners).filter(function (event) { return node.__listeners[event].length > 0; });
            if (node.__spec.field && events.indexOf('valueChange') < 0) events.push('valueChange');
            var modifier = node.__modifier === null ? [] : node.__modifier.__operations.map(function (operation) {
                return {
                    name: operation.name,
                    args: operation.args.map(function (arg, index) {
                        if (!operation.callbacks[index]) return plain(arg, 0);
                        var token = nextToken++;
                        tokens[token] = { node: node, fn: arg };
                        return { $callback: token };
                    }),
                };
            });
            var slots = {};
            Object.keys(node.__slots).forEach(function (name) { slots[name] = visit(node.__slots[name]); });
            return {
                id: node.__id, type: node.__type, key: node.__key, props: props, events: events, modifier: modifier,
                children: node.__children.map(visit), slots: slots,
            };
        }
        var tree = root ? visit(root) : null;
        snapshotNodes = nodes;
        callbacks = tokens;
        refs.forEach(function (pair) { pair[0].__node = pair[1]; });
        var pending = commands;
        commands = [];
        return JSON.stringify({ root: tree, kind: mountKind, commands: pending, fields: fields });
    }

    // ------------------------------------------------------------------ entry points for the designer

    global.__sandbox = Object.freeze({
        /** Re-renders when a state changed and returns the new snapshot, or null when nothing changed. */
        flush: function () {
            if (dirty && renderFunction) {
                dirty = false;
                render();
            }
            dirty = false;
            if (!changed) return null;
            changed = false;
            return snapshot();
        },
        /** Delivers a canvas event; [target] is a node id, or a modifier callback token. */
        dispatch: function (target, event, argsJson) {
            var args = JSON.parse(argsJson);
            if (target >= CALLBACK_BASE) {
                var entry = callbacks[target];
                if (entry) entry.fn.apply(entry.node, args);
                return;
            }
            var node = snapshotNodes[target];
            if (!node) return;
            if (node.__spec.field && event === 'valueChange' && typeof args[0] === 'string') {
                var current = editor(node);
                current.text = args[0];
                if (args[1]) { current.start = args[1].start; current.end = args[1].end; }
                if (typeof args[2] === 'number') current.seq = args[2];
                args[1] = { start: current.start, end: current.end };
            }
            (node.__listeners[event] || []).slice().forEach(function (fn) { fn.apply(node, args); });
        },
        /** Milliseconds until the next timer is due, or -1 when none is pending. */
        nextTimer: function () {
            if (!timers.length) return -1;
            var now = Date.now();
            return Math.max(0, Math.min.apply(null, timers.map(function (timer) { return timer.at; })) - now);
        },
        /** Runs the timers that are due; returns how many ran. */
        runTimers: function () {
            var now = Date.now();
            var due = timers.filter(function (timer) { return timer.at <= now; }).slice(0, 32);
            due.forEach(function (timer) {
                if (timer.every) timer.at = now + timer.every; else removeTimer(timer.id);
                // A failing timer is reported like the host reports it, and the other timers still run.
                try { timer.fn.apply(undefined, timer.args); } catch (error) { emit('error', describe(error)); }
            });
            return due.length;
        },
        mounted: function () { return root !== null; },
        /** Console lines since the last call, as JSON; a last line counts those dropped over the limit. */
        drain: function () {
            var out = lines;
            if (dropped > 0) out = out.concat([{ level: 'warn', text: dropped + ' more console lines were dropped' }]);
            lines = [];
            dropped = 0;
            return JSON.stringify(out);
        },
    });
})(this, __catalogJson);
