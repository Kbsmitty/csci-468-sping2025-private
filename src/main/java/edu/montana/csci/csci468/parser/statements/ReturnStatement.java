package edu.montana.csci.csci468.parser.statements;

import edu.montana.csci.csci468.bytecode.ByteCodeGenerator;
import edu.montana.csci.csci468.eval.CatscriptRuntime;
import edu.montana.csci.csci468.eval.ReturnException;
import edu.montana.csci.csci468.parser.*;
import edu.montana.csci.csci468.parser.expressions.Expression;
import org.objectweb.asm.Opcodes;

public class ReturnStatement extends Statement {
    private Expression expression;
    private FunctionDefinitionStatement function;

    public void setExpression(Expression parseExpression) {
        this.expression = addChild(parseExpression);
    }

    public Expression getExpression() {
        return expression;
    }

     public void setFunctionDefinition(FunctionDefinitionStatement func) {
        this.function = func;
    }

    public FunctionDefinitionStatement getFunctionDefinitionStatement() {
        if (function != null) {
            return function;
        }
        ParseElement current = this;
        while (current != null) {
            if (current instanceof FunctionDefinitionStatement) {
                function = (FunctionDefinitionStatement) current;
                return function;
            }
            current = current.getParent();
        }
        return null;
    }


    @Override
    public void validate(SymbolTable symbolTable) {
        if(function == null) {
            addError(ErrorType.INVALID_RETURN_STATEMENT);
            return;
        }

        if (expression != null) {
            expression.validate(symbolTable);
            if (!function.getType().isAssignableFrom(expression.getType())) {
                expression.addError(ErrorType.INCOMPATIBLE_TYPES);
            }
        } else {
            if (!function.getType().equals(CatscriptType.VOID)) {
                addError(ErrorType.INCOMPATIBLE_TYPES);
            }
        }
    }

    //==============================================================
    // Implementation
    //==============================================================
    @Override
    public void execute(CatscriptRuntime runtime) {
        Object value = null;
        if(expression != null){
            value = expression.evaluate(runtime);
        }
        throw new ReturnException(value);
    }

    @Override
    public void transpile(StringBuilder javascript) {
        super.transpile(javascript);
    }

    @Override
    public void compile(ByteCodeGenerator code) {
        getExpression().compile(code);
        CatscriptType type = getProgram().getFunction(function.getName()).getType();
        if(type.equals(CatscriptType.INT) || type.equals(CatscriptType.BOOLEAN)) {

            code.addInstruction(Opcodes.IRETURN);
        } else {
            box(code, getExpression().getType());
            code.addInstruction(Opcodes.ARETURN);
        }
    }
}
