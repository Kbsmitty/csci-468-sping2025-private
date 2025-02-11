package edu.montana.csci.csci468.tokenizer;

import static edu.montana.csci.csci468.tokenizer.TokenType.*;

public class CatScriptTokenizer {

    TokenList tokenList;
    String src;
    int postion = 0;
    int line = 1;
    int lineOffset = 0;

    public CatScriptTokenizer(String source) {
        src = source;
        tokenList = new TokenList(this);
        tokenize();
    }

    private void tokenize() {
        consumeWhitespace();
        while (!tokenizationEnd()) {
            scanToken();
            consumeWhitespace();
        }
        tokenList.addToken(EOF, "<EOF>", postion, postion, line, lineOffset);
    }

    private void scanToken() {
        if(scanNumber()) {
            return;
        }
        if(scanString()) {
            return;
        }
        if(scanIdentifier()) {
            return;
        }
        scanSyntax();
    }

    private boolean scanString() {
        // TODO implement string scanning here!
        if (quotesChecker(peek())) {
            int start = postion;
            StringBuilder value = new StringBuilder();
            takeChar();

            while (!tokenizationEnd() && !quotesChecker(peek())) {
                if (peek() == '\\') {
                    takeChar();
                    if (tokenizationEnd()) {
                        tokenList.addToken(ERROR, "String escape error", postion, postion, line, lineOffset);
                        return true;
                    }
                    value.append('\\').append(takeChar());
                } else {
                    value.append(takeChar());
                }
            }

            if (!tokenizationEnd()) {
                takeChar(); 
                tokenList.addToken(STRING, value.toString(), start, postion, line, lineOffset);
            } else {
                tokenList.addToken(ERROR, "Unterminated string", postion, postion, line, lineOffset);
            }
            return true;
        }
        return false;
    }

    private boolean scanIdentifier() {
        if( isAlpha(peek())) {
            int start = postion;
            while (isAlphaNumeric(peek())) {
                takeChar();
            }
            String value = src.substring(start, postion);
            if (KEYWORDS.containsKey(value)) {
                tokenList.addToken(KEYWORDS.get(value), value, start, postion, line, lineOffset);
            } else {
                tokenList.addToken(IDENTIFIER, value, start, postion, line, lineOffset);
            }
            return true;
        } else {
            return false;
        }
    }

    private boolean scanNumber() {
        if(isDigit(peek())) {
            int start = postion;
            while (isDigit(peek())) {
                takeChar();
            }
            tokenList.addToken(INTEGER, src.substring(start, postion), start, postion, line, lineOffset);
            return true;
        } else {
            return false;
        }
    }

    private void scanSyntax() {
        // TODO - implement rest of syntax scanning
        //      - implement comments

        int start = postion;
        char c = takeChar();

        switch (c) {
            case '+':
                tokenList.addToken(PLUS, "+", start, postion, line, lineOffset);
                break;
            case '-':
                tokenList.addToken(MINUS, "-", start, postion, line, lineOffset);
                break;
            case '*':
                tokenList.addToken(STAR, "*", start, postion, line, lineOffset);
                break;
            case '/':
                if (matchAndConsume('/')) { // Single-line comment
                    while (peek() != '\n' && !tokenizationEnd()) {
                        takeChar();
                    }
                } else {
                    tokenList.addToken(SLASH, "/", start, postion, line, lineOffset);
                }
                break;
            case '(': tokenList.addToken(LEFT_PAREN, "(", start, postion, line, lineOffset); break;
            case ')': tokenList.addToken(RIGHT_PAREN, ")", start, postion, line, lineOffset); break;
            case '{': tokenList.addToken(LEFT_BRACE, "{", start, postion, line, lineOffset); break;
            case '}': tokenList.addToken(RIGHT_BRACE, "}", start, postion, line, lineOffset); break;
            case '[': tokenList.addToken(LEFT_BRACKET, "[", start, postion, line, lineOffset); break;
            case ']': tokenList.addToken(RIGHT_BRACKET, "]", start, postion, line, lineOffset); break;
            case ':': tokenList.addToken(COLON, ":", start, postion, line, lineOffset); break;
            case ',': tokenList.addToken(COMMA, ",", start, postion, line, lineOffset); break;
            case '.': tokenList.addToken(DOT, ".", start, postion, line, lineOffset); break;
            case '!':
                if (matchAndConsume('=')) {
                    tokenList.addToken(BANG_EQUAL, "!=", start, postion, line, lineOffset);
                }
                break;
            case '=':
                if (matchAndConsume('=')) {
                    tokenList.addToken(EQUAL_EQUAL, "==", start, postion, line, lineOffset);
                } else {
                    tokenList.addToken(EQUAL, "=", start, postion, line, lineOffset);
                }
                break;
            case '>':
                if (matchAndConsume('=')) {
                    tokenList.addToken(GREATER_EQUAL, ">=", start, postion, line, lineOffset);
                } else {
                    tokenList.addToken(GREATER, ">", start, postion, line, lineOffset);
                }
                break;
            case '<':
                if (matchAndConsume('=')) {
                    tokenList.addToken(LESS_EQUAL, "<=", start, postion, line, lineOffset);
                } else {
                    tokenList.addToken(LESS, "<", start, postion, line, lineOffset);
                }
                break;
            default:
                tokenList.addToken(ERROR, "<Unexpected Token: [" + c + "]>", start, postion, line, lineOffset);
                break;
        }
    }

    private void consumeWhitespace() {
        // TODO update line and lineOffsets
        while (!tokenizationEnd()) {
            char c = peek();
            if (c == ' ' || c == '\r' || c == '\t') {
                lineOffset++;
                takeChar();
            } else if (c == '\n') {
                line++;
                lineOffset = 0;
                takeChar();
            } else {
                break;
            }
        }
    }

    //===============================================================
    // Utility functions
    //===============================================================

    private char peek() {
        if (tokenizationEnd()) return '\0';
        return src.charAt(postion);
    }

    private boolean quotesChecker(char c) {
        return c == '\"' || c == '\'';
    }

    private boolean filler(char c) {
        return c == ' ' || c == '\r' || c == '\t' || c == '\n';
    }

    private boolean isAlpha(char c) {
        return (c >= 'a' && c <= 'z') ||
                (c >= 'A' && c <= 'Z') ||
                c == '_';
    }

    private boolean isAlphaNumeric(char c) {
        return isAlpha(c) || isDigit(c);
    }

    private boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }

    private char takeChar() {
        char c = src.charAt(postion);
        postion++;
        if (!filler(c)) {
            lineOffset++;
        }
        return c;
    }

    private boolean tokenizationEnd() {
        return postion >= src.length();
    }

    public boolean matchAndConsume(char c) {
        if (peek() == c) {
            takeChar();
            return true;
        }
        return false;
    }

    public TokenList getTokens() {
        return tokenList;
    }

    @Override
    public String toString() {
        if (tokenizationEnd()) {
            return src + "-->[]<--";
        } else {
            return src.substring(0, postion) + "-->[" + peek() + "]<--" +
                    ((postion == src.length() - 1) ? "" :
                            src.substring(postion + 1, src.length() - 1));
        }
    }
}
