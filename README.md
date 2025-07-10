# Mini Markdown Language

**Mini Markdown Language (MML)** is a lightweight HTML-like markup language designed for structured content with tags,
attributes, and text. It includes a parser that converts MML strings into a tree of DOM nodes.

---

## 📜 Language Specification

### 1. Basic Syntax
- **Tags** are wrapped in square brackets `[ ]`:  
   ```
   [tag]Content[/tag]  
   ```
- **Attributes** use key="value" syntax inside the opening tag:
   ```
   [img url="http://example.com/image.png" alt="Description"][/img]
   ```
- **Nesting**: Tags can be nested, but must close in the correct order:
   ```
   [div][p]Text[/p][/div]  ✅ Valid  
   [div][p]Text[/div][/p]  ❌ Invalid (throws MarkdownSyntaxException)  
   ```

### 2. Supported Features
- **Text Content**: Only allowed in _leaf nodes_ (tags with no children):
  ```
  [p]This is valid text.[/p]   ✅  
  [div]Text [p]Child[/p][/div] ❌ Invalid (text mixed with child tags)
  [div][text]Text[/text] [p]Child[/p][/div] ✅
  ```
- **Attributes**:
  - Multiple attributes per tag
  - Values must be quoted (`"` or `'`). 
  - Duplicate attributes:
    ```
    [link href="#1" href="#2"][/link] -> Last value wins ("#2")
    ```
- **Whitespace**:
  - Ignored between tags/attributes (e.g., `[p] Text [/p]` → Text is trimmed).
  - Preserved inside text content (e.g., `[p]Line\nBreak[/p]` keeps \n).

---

## ⚡ Performance
- Time Complexity: **O(n)**
- Space Complexity: **O(n)**
- Call Stack Recursion: **O(d)** _(maximum nesting depth)_ **→** Usually `d ≪ n` → almost `O(1)`

---

## 🗺️ Roadmap
1. Escaping: Add escaping support for characters such as `[`, `]`, ``, `"`
2. HTML-like comments: <!-- -->