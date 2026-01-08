package com.example.movinghacker;

import androidx.annotation.NonNull;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.github.rosemoe.sora.lang.EmptyLanguage;
import io.github.rosemoe.sora.text.Content;
import io.github.rosemoe.sora.text.ContentLine;
import io.github.rosemoe.sora.widget.CodeEditor;

/**
 * 基础语法高亮器
 * 使用简单的正则表达式匹配来提供基本的语法高亮
 */
public class BasicSyntaxHighlighter {
    
    private final Set<String> keywords;
    private final Pattern keywordPattern;
    private final Pattern stringPattern;
    private final Pattern commentPattern;
    private final Pattern numberPattern;
    
    public BasicSyntaxHighlighter(String languageType) {
        this.keywords = getKeywordsForLanguage(languageType);
        
        // 构建关键字正则表达式
        if (!keywords.isEmpty()) {
            StringBuilder keywordRegex = new StringBuilder("\\b(");
            boolean first = true;
            for (String keyword : keywords) {
                if (!first) keywordRegex.append("|");
                keywordRegex.append(Pattern.quote(keyword));
                first = false;
            }
            keywordRegex.append(")\\b");
            keywordPattern = Pattern.compile(keywordRegex.toString());
        } else {
            keywordPattern = null;
        }
        
        // 字符串匹配
        stringPattern = Pattern.compile("\"([^\"\\\\]|\\\\.)*\"|'([^'\\\\]|\\\\.)*'");
        
        // 注释匹配
        commentPattern = Pattern.compile("//.*|/\\*.*?\\*/|#.*");
        
        // 数字匹配
        numberPattern = Pattern.compile("\\b\\d+(\\.\\d+)?([eE][+-]?\\d+)?\\b");
    }
    
    private Set<String> getKeywordsForLanguage(String languageType) {
        Set<String> kw = new HashSet<>();
        
        switch (languageType.toLowerCase()) {
            case "python":
                kw.addAll(Arrays.asList(
                    "False", "None", "True", "and", "as", "assert", "async", "await", 
                    "break", "class", "continue", "def", "del", "elif", "else", "except", 
                    "finally", "for", "from", "global", "if", "import", "in", "is", 
                    "lambda", "nonlocal", "not", "or", "pass", "raise", "return", 
                    "try", "while", "with", "yield"
                ));
                break;
                
            case "javascript":
            case "typescript":
                kw.addAll(Arrays.asList(
                    "abstract", "await", "boolean", "break", "byte", "case", 
                    "catch", "char", "class", "const", "continue", "debugger", "default", 
                    "delete", "do", "double", "else", "enum", "export", "extends", 
                    "false", "final", "finally", "float", "for", "function", "goto", "if", 
                    "implements", "import", "in", "instanceof", "int", "interface", "let", 
                    "long", "native", "new", "null", "package", "private", "protected", 
                    "public", "return", "short", "static", "super", "switch", "synchronized", 
                    "this", "throw", "throws", "transient", "true", "try", "typeof", "var", 
                    "void", "volatile", "while", "with", "yield"
                ));
                break;
                
            case "php":
                kw.addAll(Arrays.asList(
                    "abstract", "and", "array", "as", "break", "callable", "case", "catch", 
                    "class", "clone", "const", "continue", "declare", "default", "do", 
                    "echo", "else", "elseif", "empty", "extends", "final", 
                    "finally", "for", "foreach", "function", "global", "goto", "if", 
                    "implements", "include", "instanceof", "interface", "isset", 
                    "namespace", "new", "or", "print", "private", "protected", "public", 
                    "require", "return", "static", "switch", "throw", "trait", "try", 
                    "use", "var", "while", "yield"
                ));
                break;
                
            case "go":
                kw.addAll(Arrays.asList(
                    "break", "case", "chan", "const", "continue", "default", "defer", 
                    "else", "fallthrough", "for", "func", "go", "goto", "if", "import", 
                    "interface", "map", "package", "range", "return", "select", "struct", 
                    "switch", "type", "var"
                ));
                break;
                
            case "c":
            case "cpp":
            case "c++":
                kw.addAll(Arrays.asList(
                    "auto", "break", "case", "char", "const", "continue", "default", "do", 
                    "double", "else", "enum", "extern", "float", "for", "goto", "if", 
                    "inline", "int", "long", "register", "return", "short", 
                    "signed", "sizeof", "static", "struct", "switch", "typedef", "union", 
                    "unsigned", "void", "volatile", "while",
                    // C++ keywords
                    "class", "namespace", "template", "typename", "virtual", "public", 
                    "private", "protected", "try", "catch", "throw", "new", "delete", 
                    "this", "operator", "friend", "using", "bool", "true", "false"
                ));
                break;
                
            case "csharp":
            case "cs":
                kw.addAll(Arrays.asList(
                    "abstract", "as", "base", "bool", "break", "byte", "case", "catch", 
                    "char", "checked", "class", "const", "continue", "decimal", "default", 
                    "delegate", "do", "double", "else", "enum", "event", "explicit", 
                    "extern", "false", "finally", "fixed", "float", "for", "foreach", 
                    "goto", "if", "implicit", "in", "int", "interface", "internal", "is", 
                    "lock", "long", "namespace", "new", "null", "object", "operator", "out", 
                    "override", "params", "private", "protected", "public", "readonly", 
                    "ref", "return", "sealed", "short", "sizeof", "static", "string", 
                    "struct", "switch", "this", "throw", "true", "try", "typeof", "uint", 
                    "ulong", "using", "virtual", "void", "volatile", "while"
                ));
                break;
                
            case "kotlin":
            case "kt":
                kw.addAll(Arrays.asList(
                    "abstract", "as", "break", "by", "catch", "class", 
                    "companion", "const", "constructor", "continue", "data", 
                    "do", "else", "enum", "false", "final", "finally", "for", "fun", 
                    "if", "import", "in", "interface", "internal", "is", "null", "object", 
                    "open", "operator", "override", "package", "private", "protected", 
                    "public", "return", "sealed", "super", "this", "throw", "true", "try", 
                    "typealias", "val", "var", "when", "while"
                ));
                break;
                
            case "swift":
                kw.addAll(Arrays.asList(
                    "class", "deinit", "enum", "extension", "func", "import", "init", 
                    "internal", "let", "operator", "private", "protocol", "public", 
                    "static", "struct", "subscript", "typealias", "var", "break", "case", 
                    "continue", "default", "defer", "do", "else", "fallthrough", "for", 
                    "guard", "if", "in", "repeat", "return", "switch", "where", "while", 
                    "as", "catch", "false", "is", "nil", "super", "self", "Self", "throw", 
                    "throws", "true", "try"
                ));
                break;
                
            case "ruby":
            case "rb":
                kw.addAll(Arrays.asList(
                    "BEGIN", "END", "alias", "and", "begin", "break", "case", "class", 
                    "def", "do", "else", "elsif", "end", "ensure", "false", 
                    "for", "if", "in", "module", "next", "nil", "not", "or", "redo", 
                    "rescue", "retry", "return", "self", "super", "then", "true", "undef", 
                    "unless", "until", "when", "while", "yield"
                ));
                break;
                
            case "rust":
            case "rs":
                kw.addAll(Arrays.asList(
                    "as", "break", "const", "continue", "crate", "else", "enum", "extern", 
                    "false", "fn", "for", "if", "impl", "in", "let", "loop", "match", 
                    "mod", "move", "mut", "pub", "ref", "return", "self", "Self", "static", 
                    "struct", "super", "trait", "true", "type", "unsafe", "use", "where", 
                    "while", "async", "await", "dyn"
                ));
                break;
                
            case "sql":
                kw.addAll(Arrays.asList(
                    "SELECT", "FROM", "WHERE", "INSERT", "UPDATE", "DELETE", "CREATE", 
                    "ALTER", "DROP", "TABLE", "INDEX", "VIEW", "JOIN", "INNER", "LEFT", 
                    "RIGHT", "OUTER", "ON", "AND", "OR", "NOT", "NULL", "IS", "IN", 
                    "BETWEEN", "LIKE", "ORDER", "BY", "GROUP", "HAVING", "LIMIT", "OFFSET", 
                    "AS", "DISTINCT", "COUNT", "SUM", "AVG", "MAX", "MIN", "PRIMARY", 
                    "KEY", "FOREIGN", "REFERENCES", "UNIQUE", "DEFAULT", "VALUES", "INTO"
                ));
                break;
                
            case "bash":
            case "sh":
                kw.addAll(Arrays.asList(
                    "if", "then", "else", "elif", "fi", "case", "esac", "for", "select", 
                    "while", "until", "do", "done", "in", "function", "time", 
                    "break", "continue", "return", "exit", "export", "readonly", 
                    "local", "declare", "unset", "source", "eval", "exec", "true", "false"
                ));
                break;
        }
        
        return kw;
    }
    
    public Set<String> getKeywords() {
        return keywords;
    }
}
