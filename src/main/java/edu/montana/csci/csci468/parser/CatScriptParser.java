package edu.montana.csci.csci468.parser;

import edu.montana.csci.csci468.parser.expressions.*;
import edu.montana.csci.csci468.parser.statements.*;
import edu.montana.csci.csci468.tokenizer.CatScriptTokenizer;
import edu.montana.csci.csci468.tokenizer.Token;
import edu.montana.csci.csci468.tokenizer.TokenList;
import edu.montana.csci.csci468.tokenizer.TokenType;

import static edu.montana.csci.csci468.tokenizer.TokenType.*;

public class CatScriptParser {

    private TokenList tokens;

    public CatScriptProgram parse(String source) {
        tokens = new CatScriptTokenizer(source).getTokens();

        // first parse an expression
        CatScriptProgram program = new CatScriptProgram();
        program.setStart(tokens.getCurrentToken());
        Expression expression = null;
        try {
            expression = parseExpression();
        } catch(RuntimeException re) {
            // ignore :)
        }
        if (expression == null || tokens.hasMoreTokens()) {
            tokens.reset();
            while (tokens.hasMoreTokens()) {
                program.addStatement(parseProgramStatement());
            }
        } else {
            program.setExpression(expression);
        }

        program.setEnd(tokens.getCurrentToken());
        return program;
    }

    public CatScriptProgram parseAsExpression(String source) {
        tokens = new CatScriptTokenizer(source).getTokens();
        CatScriptProgram program = new CatScriptProgram();
        program.setStart(tokens.getCurrentToken());
        Expression expression = parseExpression();
        program.setExpression(expression);
        program.setEnd(tokens.getCurrentToken());
        return program;
    }

    //============================================================
    //  Statements
    //============================================================

    private Statement parseProgramStatement() {
        Statement printStmt = parsePrintStatement();
        if (printStmt != null) {
            return printStmt;
        }
        return new SyntaxErrorStatement(tokens.consumeToken());
    }

    private Statement parsePrintStatement() {
        if (tokens.match(PRINT)) {

            PrintStatement printStatement = new PrintStatement();
            printStatement.setStart(tokens.consumeToken());

            require(LEFT_PAREN, printStatement);
            printStatement.setExpression(parseExpression());
            printStatement.setEnd(require(RIGHT_PAREN, printStatement));

            return printStatement;
        } else {
            return null;
        }
    }

    //============================================================
    //  Expressions
    //============================================================

    private Expression parseExpression() {
        Expression expression = parseComparisonExpression();

        while (tokens.match(EQUAL_EQUAL, BANG_EQUAL)) {
            Token operator = tokens.consumeToken();
            Expression rightHandSide = parseComparisonExpression();
            expression = new EqualityExpression(operator, expression, rightHandSide);
        }
        return expression;
    }

    private Expression parseComparisonExpression() {
        Expression expression = parseAdditiveExpression();

        while (tokens.match(LESS, LESS_EQUAL, GREATER, GREATER_EQUAL)) {
            Token operator = tokens.consumeToken();
            Expression rightHandSide = parseAdditiveExpression();
            expression = new ComparisonExpression(operator, expression, rightHandSide);
        }
        return expression;
    }

    private Expression parseAdditiveExpression() {
        Expression expression = parseFactorExpression();

        while (tokens.match(PLUS, MINUS)) {
            Token operator = tokens.consumeToken();
            Expression rightHandSide = parseFactorExpression();
            expression = new AdditiveExpression(operator, expression, rightHandSide);
        }
        return expression;
    }

    private Expression parseFactorExpression() {
        Expression expression = parseUnaryExpression();

        while (tokens.match(STAR, SLASH)) {
            Token operator = tokens.consumeToken();
            Expression rightHandSide = parseUnaryExpression();
            expression = new FactorExpression(operator, expression, rightHandSide);
        }
        return expression;
    }

    private Expression parseUnaryExpression() {
        if (tokens.match(MINUS, NOT)) {
            Token token = tokens.consumeToken();
            Expression rhs = parseUnaryExpression();
            UnaryExpression unaryExpression = new UnaryExpression(token, rhs);
            unaryExpression.setStart(token);
            unaryExpression.setEnd(rhs.getEnd());
            return unaryExpression;
        } else {
            return parsePrimaryExpression();
        }
    }

    private Expression parsePrimaryExpression() {
        if (tokens.match(INTEGER)) {
            Token integerToken = tokens.consumeToken();
            IntegerLiteralExpression integerExpression = new IntegerLiteralExpression(integerToken.getStringValue());
            integerExpression.setToken(integerToken);
            return integerExpression;
        } else if (tokens.match(STRING)) {
            Token stringToken = tokens.consumeToken();
            StringLiteralExpression stringExpression = new StringLiteralExpression(stringToken.getStringValue());
            stringExpression.setToken(stringToken);
            return stringExpression;
        } else if (tokens.match(LEFT_PAREN)) {
            Token token = tokens.consumeToken();
            Expression expr = parseExpression();
            ParenthesizedExpression parenthesizedExpression = new ParenthesizedExpression(expr);
            parenthesizedExpression.setStart(token);
            Token endToken = require(RIGHT_PAREN, parenthesizedExpression);
            parenthesizedExpression.setEnd(endToken);
            return parenthesizedExpression;
        } else if (tokens.match(LEFT_BRACKET)) {
            Token start = tokens.consumeToken();
            List<Expression> values = new ArrayList<>();

            if (!tokens.match(RIGHT_BRACKET)) {
                do {
                    values.add(parseExpression());
                } while (tokens.matchAndConsume(COMMA));
            }

            Token endToken = tokens.match(RIGHT_BRACKET) ? tokens.consumeToken() : null;
            ListLiteralExpression llExpression = new ListLiteralExpression(values);
            llExpression.setStart(start);
            if (endToken != null) {
                llExpression.setEnd(endToken);
            } else {
                llExpression.addError(ErrorType.UNTERMINATED_LIST, tokens.getCurrentToken());
            }
            return llExpression;
        } else if (tokens.match(IDENTIFIER)) {
            Token start = tokens.consumeToken();

            if (tokens.match(LEFT_PAREN)) {
                List<Expression> args = new ArrayList<>();
                tokens.consumeToken(); // Consume '('

                if (!tokens.match(RIGHT_PAREN)) {
                    do {
                        args.add(parseExpression());
                    } while (tokens.matchAndConsume(COMMA));
                }

                Token end = tokens.match(RIGHT_PAREN) ? tokens.consumeToken() : null;
                FunctionCallExpression functionCall = new FunctionCallExpression(start.getStringValue(), args);
                functionCall.setStart(start);
                if (end != null) {
                    functionCall.setEnd(end);
                } else {
                    functionCall.addError(ErrorType.UNTERMINATED_ARG_LIST, tokens.getCurrentToken());
                }
                return functionCall;
            } else {
                IdentifierExpression identifierExpression = new IdentifierExpression(start.getStringValue());
                identifierExpression.setToken(start);
                return identifierExpression;
            }
        } else if (tokens.match(TRUE)) {
            Token trueToken = tokens.consumeToken();
            BooleanLiteralExpression trueExpression = new BooleanLiteralExpression(true);
            trueExpression.setToken(trueToken);
            return trueExpression;
        } else if (tokens.match(FALSE)) {
            Token falseToken = tokens.consumeToken();
            BooleanLiteralExpression falseExpression = new BooleanLiteralExpression(false);
            falseExpression.setToken(falseToken);
            return falseExpression;
        } else if (tokens.match(NULL)) {
            Token nullToken = tokens.consumeToken();
            NullLiteralExpression nullExpression = new NullLiteralExpression();
            nullExpression.setToken(nullToken);
            return nullExpression;
        } else {
            SyntaxErrorExpression syntaxErrorExpression = new SyntaxErrorExpression(tokens.consumeToken());
            return syntaxErrorExpression;
        }
    }

    //============================================================
    //  Parse Helpers
    //============================================================
    private Token require(TokenType type, ParseElement elt) {
        return require(type, elt, ErrorType.UNEXPECTED_TOKEN);
    }

    private Token require(TokenType type, ParseElement elt, ErrorType msg) {
        if(tokens.match(type)){
            return tokens.consumeToken();
        } else {
            elt.addError(msg, tokens.getCurrentToken());
            return tokens.getCurrentToken();
        }
    }

}
