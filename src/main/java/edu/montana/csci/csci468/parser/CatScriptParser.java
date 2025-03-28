package edu.montana.csci.csci468.parser;

import edu.montana.csci.csci468.parser.expressions.*;
import edu.montana.csci.csci468.parser.statements.*;
import edu.montana.csci.csci468.tokenizer.CatScriptTokenizer;
import edu.montana.csci.csci468.tokenizer.Token;
import edu.montana.csci.csci468.tokenizer.TokenList;
import edu.montana.csci.csci468.tokenizer.TokenType;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import static edu.montana.csci.csci468.tokenizer.TokenType.*;

public class CatScriptParser {

    private TokenList tokens;
    private FunctionDefinitionStatement currentFunctionDefinition;

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
        Statement funcDefstmt = parseFunctionDefintionStatement();
        if (funcDefstmt != null) {
            return funcDefstmt;
        }
        return parseStatement();
    }

    private Statement parseFunctionDefintionStatement(){
        if (tokens.match(FUNCTION)) {
            FunctionDefinitionStatement funcDef = new FunctionDefinitionStatement();
            funcDef.setStart((tokens.consumeToken()));
            if(tokens.match(IDENTIFIER)){
                funcDef.setName(tokens.consumeToken().getStringValue());
                require(LEFT_PAREN,funcDef);
                if (!tokens.match(RIGHT_PAREN)) {
                    do {
                        Token id = require(IDENTIFIER,funcDef);
                        TypeLiteral typeLiteral = null;
                        if (tokens.matchAndConsume(COLON)) {
                            typeLiteral = parseTypeExpression();
                        }
                        funcDef.addParameter(id.getStringValue(), typeLiteral);

                    } while (tokens.matchAndConsume(COMMA));
                }

                require(RIGHT_PAREN,funcDef);
                if(tokens.matchAndConsume(COLON)){
                    funcDef.setType(parseTypeExpression());

                }else{
                    TypeLiteral typeLiteral = new TypeLiteral();
                    typeLiteral.setType(CatscriptType.VOID);
                    funcDef.setType(typeLiteral);
                }
            }
            require(LEFT_BRACE,funcDef);

            List<Statement> statementList = new LinkedList();

            this.currentFunctionDefinition = funcDef;
            while (!tokens.match(RIGHT_BRACE) && tokens.hasMoreTokens()) {
                if (tokens.match(RETURN)) {

                    statementList.add(parseReturnStatement());
                } else {
                    statementList.add(parseStatement());
                }

            }
            funcDef.setBody(statementList);
            this.currentFunctionDefinition = null;
            require(RIGHT_BRACE, funcDef);

            return funcDef;

        }
        return null;
    }


    private TypeLiteral parseTypeExpression(){

        TypeLiteral typeLiteral = new TypeLiteral();
        Token typeToken = tokens.consumeToken();
        if(typeToken.getStringValue().equals("int")){
            typeLiteral.setType(CatscriptType.INT);
        } else if (typeToken.getStringValue().equals("string")) {
            typeLiteral.setType(CatscriptType.STRING);
        } else if (typeToken.getStringValue().equals("bool")) {
            typeLiteral.setType(CatscriptType.BOOLEAN);
        } else if (typeToken.getStringValue().equals("object")) {
            typeLiteral.setType(CatscriptType.OBJECT);
        } else if (typeToken.getStringValue().equals("list")) {

            typeLiteral.setType(CatscriptType.getListType(CatscriptType.OBJECT));
            if(tokens.matchAndConsume(LESS)){
                typeLiteral.setType(CatscriptType.getListType(parseTypeExpression().getType()));
                require(GREATER,typeLiteral);
            }

        }
        else{
            typeLiteral.setType(CatscriptType.getListType(CatscriptType.OBJECT));
        }
        
        return typeLiteral;
    }
    private Statement parseStatement() {
        System.out.println("Parsing function definition, current token: " + tokens.getCurrentToken());
        try{
            Statement printStmt = parsePrintStatement();
            if (printStmt != null) {
                return printStmt;
            }
            Statement forStmt = parseForStatement();
            if (forStmt != null) {
                return forStmt;
            }
            Statement ifStmt = parseIfStatement();
            if(ifStmt!=null){
                return ifStmt;
            }

            Statement returnStmt= parseReturnStatement();{
                if(returnStmt != null){
                    return returnStmt;
                }
            }
            Statement assignmentOrFuncCall = parseAssignmentOrFunctionCallStatement();
            if(assignmentOrFuncCall!=null){
                return  assignmentOrFuncCall;
            }
            Statement variableStatement = parseVariableStatement();
            if(variableStatement!=null){
                return variableStatement;
            }
            if(currentFunctionDefinition !=null){
                Statement stmt =parseReturnStatement();
                if(stmt!=null){
                    return stmt;
                }
            }
            throwParseError();
            return null;
        }catch (CatscriptParseException catScriptParseException) {
            SyntaxErrorStatement syntaxErrorStatement = new SyntaxErrorStatement(catScriptParseException.getToken());
            synchronizeTokens();
            return syntaxErrorStatement;

        }
    }

    private void synchronizeTokens() {
        while(tokens.hasMoreTokens() && !(tokens.match(FUNCTION,FOR,IF,VAR,PRINT,RETURN))){
            tokens.consumeToken();
        }
    }

    private void throwParseError(){
        throw new CatscriptParseException(tokens.consumeToken());
    }

    private Statement parseVariableStatement() {
        VariableStatement variableStatement = new VariableStatement();
        if(tokens.match(VAR)){
            variableStatement.setStart(tokens.consumeToken());

            if(tokens.match(IDENTIFIER)){
                variableStatement.setVariableName(tokens.consumeToken().getStringValue());

                if(tokens.matchAndConsume(COLON)){
                    TypeLiteral type = parseTypeExpression();


                    variableStatement.setExplicitType(type.getType());
                }
                require(EQUAL,variableStatement);
                variableStatement.setExpression(parseExpression());
                return variableStatement; }

        }
        return null;
    }

    private Statement parseIfStatement() {
        IfStatement ifStatement = new IfStatement();
        if(tokens.match(IF)){
            ifStatement.setStart(tokens.consumeToken());
            require(LEFT_PAREN,ifStatement);
            ifStatement.setExpression(parseExpression());
            require(RIGHT_PAREN,ifStatement);
            require(LEFT_BRACE,ifStatement);
            List<Statement> statementList = new LinkedList();
            Statement pieceStmt = null;
            while(tokens.hasMoreTokens()&&!tokens.match(RIGHT_BRACE)) {
                pieceStmt=parseStatement();
                statementList.add(pieceStmt);
            }

            ifStatement.setTrueStatements(statementList);
            require(RIGHT_BRACE,ifStatement);
            if(tokens.matchAndConsume(ELSE)){

                require(LEFT_BRACE,ifStatement);
                List<Statement> statementListElse = new LinkedList();
                Statement pieceStmtElse = null;
                while(tokens.hasMoreTokens()&&!tokens.match(RIGHT_BRACE)) {
                    pieceStmtElse=parseStatement();
                    statementListElse.add(pieceStmtElse);
                }

                ifStatement.setElseStatements(statementListElse);
                require(RIGHT_BRACE,ifStatement,ErrorType.UNEXPECTED_TOKEN);

            }
            return ifStatement;
        }
        return null;
    }

    private Statement parseAssignmentOrFunctionCallStatement() {
        if(tokens.match(IDENTIFIER)){
            Token id = tokens.consumeToken();
            if(tokens.match(LEFT_PAREN)) {
                return parseFunctionCallStatement(id);
            }else{
                return parseAssignmentStatement(id);
            }
        }
        return null;
    }
    private Statement parseAssignmentStatement(Token id){
        AssignmentStatement assignmentStatement = new AssignmentStatement();
        assignmentStatement.setStart(id);
        require(EQUAL,assignmentStatement);
        assignmentStatement.setVariableName(id.getStringValue());
        assignmentStatement.setExpression(parseExpression());
        return assignmentStatement;


    }
    private Statement parseFunctionCallStatement(Token id){
        if(tokens.matchAndConsume(LEFT_PAREN)){
            List<Expression> expressionList = new LinkedList<>();


            if(tokens.matchAndConsume(RIGHT_PAREN)){
                FunctionCallExpression functionCallExpression = new FunctionCallExpression(id.getStringValue(), expressionList);
                FunctionCallStatement functionCallStatement = new FunctionCallStatement(functionCallExpression);
                functionCallStatement.setStart(id);
                return functionCallStatement;
            }else{
                do{
                    expressionList.add(parseExpression());
                }while(tokens.matchAndConsume(COMMA));
                FunctionCallExpression functionCallExpression = new FunctionCallExpression(id.getStringValue(), expressionList);
                FunctionCallStatement functionCallStatement = new FunctionCallStatement(functionCallExpression);
                functionCallStatement.setStart(id);
                require(RIGHT_PAREN,functionCallStatement);

                return functionCallStatement;
            }


        }


        return null;
    }
    private Statement parseForStatement(){
        ForStatement forStatement = new ForStatement();
        if(tokens.match(FOR)){
            forStatement.setStart(tokens.consumeToken());
            require(LEFT_PAREN,forStatement);
            if(tokens.match(IDENTIFIER)){
                forStatement.setVariableName(tokens.consumeToken().getStringValue());
                require(IN,forStatement);
                forStatement.setExpression(parseExpression());
                require(RIGHT_PAREN,forStatement);
                require(LEFT_BRACE,forStatement);
                List<Statement> statementList = new LinkedList();
                Statement pieceStmt = null;
                while(tokens.hasMoreTokens()&&!tokens.match(RIGHT_BRACE)) {
                    pieceStmt=parseStatement();
                    statementList.add(pieceStmt);
                }

                forStatement.setBody(statementList);
                require(RIGHT_BRACE,forStatement,ErrorType.UNEXPECTED_TOKEN);
                return forStatement;


            }

        }else{
            //no opening left paren{ for ( <--  }
        }

        return null;
    }
    private Statement parseReturnStatement() {
        if(this.currentFunctionDefinition!=null){
            ReturnStatement returnStatement = new ReturnStatement();
            if(tokens.match(RETURN)){
                returnStatement.setStart(tokens.consumeToken());

                returnStatement.setFunctionDefinition(this.currentFunctionDefinition);
                if(tokens.hasMoreTokens()&&!tokens.match(RIGHT_BRACE)){
                    returnStatement.setExpression(parseExpression());
                }
                return returnStatement;
            }
        }
        return null;
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
