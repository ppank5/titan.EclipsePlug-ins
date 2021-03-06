/******************************************************************************
 * Copyright (c) 2000-2021 Ericsson Telecom AB
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/epl-2.0/EPL-2.0.html
 ******************************************************************************/
package org.eclipse.titan.designer.AST.TTCN3.definitions;

import java.text.MessageFormat;
import java.util.List;
import java.util.Locale;

import org.eclipse.titan.designer.AST.ASTVisitor;
import org.eclipse.titan.designer.AST.GovernedSimple.CodeSectionType;
import org.eclipse.titan.designer.AST.INamedNode;
import org.eclipse.titan.designer.AST.IReferenceChain;
import org.eclipse.titan.designer.AST.ISubReference;
import org.eclipse.titan.designer.AST.ISubReference.Subreference_type;
import org.eclipse.titan.designer.AST.IType;
import org.eclipse.titan.designer.AST.IType.TypeOwner_type;
import org.eclipse.titan.designer.AST.IType.Type_type;
import org.eclipse.titan.designer.AST.IType.ValueCheckingOptions;
import org.eclipse.titan.designer.AST.IValue;
import org.eclipse.titan.designer.AST.IValue.Value_type;
import org.eclipse.titan.designer.AST.Identifier;
import org.eclipse.titan.designer.AST.Location;
import org.eclipse.titan.designer.AST.NamingConventionHelper;
import org.eclipse.titan.designer.AST.ReferenceFinder;
import org.eclipse.titan.designer.AST.ReferenceFinder.Hit;
import org.eclipse.titan.designer.AST.Scope;
import org.eclipse.titan.designer.AST.Type;
import org.eclipse.titan.designer.AST.Value;
import org.eclipse.titan.designer.AST.TTCN3.Expected_Value_type;
import org.eclipse.titan.designer.AST.TTCN3.attributes.MultipleWithAttributes;
import org.eclipse.titan.designer.AST.TTCN3.attributes.WithAttributesPath;
import org.eclipse.titan.designer.AST.TTCN3.definitions.FormalParameter.parameterEvaluationType;
import org.eclipse.titan.designer.AST.TTCN3.types.Array_Type;
import org.eclipse.titan.designer.AST.TTCN3.types.ComponentTypeBody;
import org.eclipse.titan.designer.AST.TTCN3.values.expressions.ExpressionStruct;
import org.eclipse.titan.designer.compiler.JavaGenData;
import org.eclipse.titan.designer.editors.ProposalCollector;
import org.eclipse.titan.designer.editors.actions.DeclarationCollector;
import org.eclipse.titan.designer.parsers.CompilationTimeStamp;
import org.eclipse.titan.designer.parsers.ttcn3parser.IIdentifierReparser;
import org.eclipse.titan.designer.parsers.ttcn3parser.IdentifierReparser;
import org.eclipse.titan.designer.parsers.ttcn3parser.ReParseException;
import org.eclipse.titan.designer.parsers.ttcn3parser.TTCN3ReparseUpdater;
import org.eclipse.titan.designer.parsers.ttcn3parser.Ttcn3Lexer;
import org.eclipse.titan.designer.preferences.PreferenceConstants;

/**
 * The Def_Var class represents TTCN3 variable definitions.
 *
 * @author Kristof Szabados
 * @author Arpad Lovassy
 */
public final class Def_Var extends Definition {
	private static final String FULLNAMEPART1 = ".<type>";
	private static final String FULLNAMEPART2 = ".<initial_value>";
	public static final String PORTNOTALLOWED = "Variable can not be defined for port type `{0}''";
	public static final String SIGNATURENOTALLOWED = "Variable can not be defined for signature `{0}''";

	private static final String KIND = " variable definition";

	private final Type type;
	private final Value initialValue;

	private boolean wasAssigned;

	/**
	 * normal, lazy or fuzzy evaluation should be used.
	 */
	private final parameterEvaluationType evaluationType;

	public Def_Var( final Identifier identifier, final Type type, final Value initialValue, final parameterEvaluationType evaluationType ) {
		super(identifier);
		this.type = type;
		this.initialValue = initialValue;
		this.evaluationType = evaluationType;

		if (type != null) {
			type.setOwnertype(TypeOwner_type.OT_VAR_DEF, this);
			type.setFullNameParent(this);
		}
		if (initialValue != null) {
			initialValue.setFullNameParent(this);
		}
	}

	@Override
	/** {@inheritDoc} */
	public Assignment_type getAssignmentType() {
		return Assignment_type.A_VAR;
	}

	@Override
	/** {@inheritDoc} */
	public StringBuilder getFullName(final INamedNode child) {
		final StringBuilder builder = super.getFullName(child);

		if (type == child) {
			return builder.append(FULLNAMEPART1);
		} else if (initialValue == child) {
			return builder.append(FULLNAMEPART2);
		}

		return builder;
	}

	@Override
	/** {@inheritDoc} */
	public void setMyScope(final Scope scope) {
		super.setMyScope(scope);
		if (type != null) {
			type.setMyScope(scope);
		}
		if (initialValue != null) {
			initialValue.setMyScope(scope);
		}
	}

	@Override
	/** {@inheritDoc} */
	public String getAssignmentName() {
		return "variable";
	}

	@Override
	/** {@inheritDoc} */
	public String getDescription() {
		final StringBuilder builder = new StringBuilder();
		builder.append(getAssignmentName()).append(" `");

		if (isLocal()) {
			builder.append(identifier.getDisplayName());
		} else {
			builder.append(getFullName());
		}

		builder.append('\'');
		return builder.toString();
	}

	@Override
	/** {@inheritDoc} */
	public String getOutlineIcon() {
		return "variable.gif";
	}

	@Override
	/** {@inheritDoc} */
	public int category() {
		int result = super.category();
		if (type != null) {
			result += type.category();
		}
		return result;
	}

	@Override
	/** {@inheritDoc} */
	public Type getType(final CompilationTimeStamp timestamp) {
		check(timestamp);

		return type;
	}

	public Value getInitialValue() {
		return initialValue;
	}

	@Override
	/** {@inheritDoc} */
	public void setWithAttributes(final MultipleWithAttributes attributes) {
		// variable should not have with attributes
	}

	@Override
	/** {@inheritDoc} */
	public void setAttributeParentPath(final WithAttributesPath parent) {
		// variable should not have with attributes
	}

	@Override
	/** {@inheritDoc} */
	public void check(final CompilationTimeStamp timestamp) {
		check(timestamp, null);
	}

	@Override
	/** {@inheritDoc} */
	public void check(final CompilationTimeStamp timestamp, final IReferenceChain refChain) {
		if (lastTimeChecked != null && !lastTimeChecked.isLess(timestamp)) {
			return;
		}
		lastTimeChecked = timestamp;

		isUsed = false;
		wasAssigned = false;

		if (getMyScope() instanceof ComponentTypeBody) {
			NamingConventionHelper.checkConvention(PreferenceConstants.REPORTNAMINGCONVENTION_COMPONENT_VARIABLE, identifier, this);
		} else {
			NamingConventionHelper.checkConvention(PreferenceConstants.REPORTNAMINGCONVENTION_LOCAL_VARIABLE, identifier, this);
		}
		NamingConventionHelper.checkNameContents(identifier, getMyScope().getModuleScope().getIdentifier(), getDescription());

		if (type == null) {
			return;
		}

		type.setGenName("_T_", getGenName());
		type.check(timestamp);

		final IType lastType = type.getTypeRefdLast(timestamp);
		switch (lastType.getTypetype()) {
		case TYPE_PORT:
			location.reportSemanticError(MessageFormat.format(PORTNOTALLOWED, lastType.getFullName()));
			break;
		case TYPE_SIGNATURE:
			location.reportSemanticError(MessageFormat.format(SIGNATURENOTALLOWED, lastType.getFullName()));
			break;
		default:
			break;
		}

		if (initialValue != null) {
			initialValue.setMyGovernor(type);
			final IValue temporalValue = type.checkThisValueRef(timestamp, initialValue);
			if (isLocal()) {
				type.checkThisValue(timestamp, temporalValue, this, new ValueCheckingOptions(Expected_Value_type.EXPECTED_DYNAMIC_VALUE,
						true, false, true, false, false));
			} else {
				type.checkThisValue(timestamp, temporalValue, this, new ValueCheckingOptions(Expected_Value_type.EXPECTED_STATIC_VALUE,
						true, false, true, false, false));
			}

			initialValue.setGenNameRecursive(getGenName());
			initialValue.setCodeSection(CodeSectionType.CS_INLINE);
		}
	}

	/**
	 * Indicates that this variable was used in a way where its value can be
	 * changed.
	 * */
	public void setWritten() {
		wasAssigned = true;
	}

	@Override
	/** {@inheritDoc} */
	public boolean checkIdentical(final CompilationTimeStamp timestamp, final Definition definition) {
		check(timestamp);
		definition.check(timestamp);

		if (!Assignment_type.A_VAR.semanticallyEquals(definition.getAssignmentType())) {
			location.reportSemanticError(MessageFormat.format(
					"Local definition `{0}'' is a variable, but the definition inherited from component type `{1}'' is a {2}",
					identifier.getDisplayName(), definition.getMyScope().getFullName(), definition.getAssignmentName()));
			return false;
		}

		final Def_Var otherVariable = (Def_Var) definition;
		if (!type.isIdentical(timestamp, otherVariable.type)) {
			final String message = MessageFormat
					.format("Local variable `{0}'' has type `{1}'', but the variable inherited from component type `{2}'' has type `{3}''",
							identifier.getDisplayName(), type.getTypename(), otherVariable.getMyScope().getFullName(),
							otherVariable.type.getTypename());
			type.getLocation().reportSemanticError(message);
			return false;
		}

		if (initialValue != null) {
			if (otherVariable.initialValue != null) {
				if (!initialValue.isUnfoldable(timestamp) && !otherVariable.initialValue.isUnfoldable(timestamp)
						&& !initialValue.checkEquality(timestamp, otherVariable.initialValue)) {
					final String message = MessageFormat
							.format("Local variable `{0}'' and the variable inherited from component type `{1}'' have different values",
									identifier.getDisplayName(), otherVariable.getMyScope().getFullName());
					initialValue.getLocation().reportSemanticWarning(message);
				}
			} else {
				initialValue.getLocation()
				.reportSemanticWarning(
						MessageFormat.format(
								"Local variable `{0}'' has initial value, but the variable inherited from component type `{1}'' does not",
								identifier.getDisplayName(), otherVariable.getMyScope().getFullName()));
			}
		} else if (otherVariable.initialValue != null) {
			location.reportSemanticWarning(MessageFormat
					.format("Local variable `{0}'' does not have initial value, but the variable inherited from component type `{1}'' has",
							identifier.getDisplayName(), otherVariable.getMyScope().getFullName()));
		}

		return true;
	}

	@Override
	/** {@inheritDoc} */
	public String getProposalKind() {
		final StringBuilder builder = new StringBuilder();
		if (type != null) {
			type.getProposalDescription(builder);
		}
		builder.append(KIND);
		return builder.toString();
	}

	@Override
	/** {@inheritDoc} */
	public void addProposal(final ProposalCollector propCollector, final int index) {
		final List<ISubReference> subrefs = propCollector.getReference().getSubreferences();
		if (subrefs.size() <= index) {
			return;
		}

		if (subrefs.size() == index + 1 && identifier.getName().toLowerCase(Locale.ENGLISH).startsWith(subrefs.get(index).getId().getName().toLowerCase(Locale.ENGLISH))) {
			super.addProposal(propCollector, index);
		} else if (subrefs.size() > index + 1 && type != null && identifier.getName().equals(subrefs.get(index).getId().getName())) {
			// perfect match
			type.addProposal(propCollector, index + 1);
		}
	}

	@Override
	/** {@inheritDoc} */
	public void addDeclaration(final DeclarationCollector declarationCollector, final int index) {
		final List<ISubReference> subrefs = declarationCollector.getReference().getSubreferences();
		if (subrefs.size() > index && identifier.getName().equals(subrefs.get(index).getId().getName())) {
			if (subrefs.size() > index + 1 && type != null) {
				type.addDeclaration(declarationCollector, index + 1);
			} else if (subrefs.size() == index + 1 && Subreference_type.fieldSubReference.equals(subrefs.get(index).getReferenceType())) {
				declarationCollector.addDeclaration(this);
			}
		}
	}

	@Override
	/** {@inheritDoc} */
	public List<Integer> getPossibleExtensionStarterTokens() {
		final List<Integer> result = super.getPossibleExtensionStarterTokens();

		if (initialValue == null) {
			result.add(Ttcn3Lexer.ASSIGNMENTCHAR);
		}

		return result;
	}

	@Override
	/** {@inheritDoc} */
	public void updateSyntax(final TTCN3ReparseUpdater reparser, final boolean isDamaged) throws ReParseException {
		if (isDamaged) {
			lastTimeChecked = null;
			boolean enveloped = false;
			int result = 1;

			final Location temporalIdentifier = identifier.getLocation();
			if (reparser.envelopsDamage(temporalIdentifier) || reparser.isExtending(temporalIdentifier)) {
				reparser.extendDamagedRegion(temporalIdentifier);
				final IIdentifierReparser r = new IdentifierReparser(reparser);
				result = r.parseAndSetNameChanged();
				identifier = r.getIdentifier();
				// damage handled
				if (result == 0 && identifier != null) {
					enveloped = true;
				} else {
					throw new ReParseException(result);
				}
			}

			if (type != null) {
				if (enveloped) {
					type.updateSyntax(reparser, false);
					reparser.updateLocation(type.getLocation());
				} else if (reparser.envelopsDamage(type.getLocation())) {
					type.updateSyntax(reparser, true);
					enveloped = true;
					reparser.updateLocation(type.getLocation());
				}
			}

			if (initialValue != null) {
				if (enveloped) {
					initialValue.updateSyntax(reparser, false);
					reparser.updateLocation(initialValue.getLocation());
				} else if (reparser.envelopsDamage(initialValue.getLocation())) {
					initialValue.updateSyntax(reparser, true);
					enveloped = true;
					reparser.updateLocation(initialValue.getLocation());
				}
			}

			if (!enveloped) {
				throw new ReParseException();
			}

			return;
		}

		reparser.updateLocation(identifier.getLocation());
		if (type != null) {
			type.updateSyntax(reparser, false);
			reparser.updateLocation(type.getLocation());
		}

		if (initialValue != null) {
			initialValue.updateSyntax(reparser, false);
			reparser.updateLocation(initialValue.getLocation());
		}
	}

	@Override
	/** {@inheritDoc} */
	public void findReferences(final ReferenceFinder referenceFinder, final List<Hit> foundIdentifiers) {
		super.findReferences(referenceFinder, foundIdentifiers);
		if (type != null) {
			type.findReferences(referenceFinder, foundIdentifiers);
		}
		if (initialValue != null) {
			initialValue.findReferences(referenceFinder, foundIdentifiers);
		}
	}

	@Override
	/** {@inheritDoc} */
	protected boolean memberAccept(final ASTVisitor v) {
		if (!super.memberAccept(v)) {
			return false;
		}
		if (type != null && !type.accept(v)) {
			return false;
		}
		if (initialValue != null && !initialValue.accept(v)) {
			return false;
		}
		return true;
	}

	public boolean getWritten() {
		return wasAssigned;
	}

	/**
	 * @return how this variable should be evaluated.
	 */
	public parameterEvaluationType get_eval_type() {
		return evaluationType;
	}

	@Override
	/** {@inheritDoc} */
	public void generateCode( final JavaGenData aData, final boolean cleanUp ) {
		final String genName = getGenName();
		final StringBuilder sb = aData.getSrc();
		final StringBuilder source = new StringBuilder();
		final StringBuilder initComp = aData.getInitComp();
		if ( !isLocal() ) {
			source.append( "\tpublic static final " );
		}

		final String typeGeneratedName = type.getGenNameValue( aData, source );
		if (type.getTypetype() == Type_type.TYPE_ARRAY) {
			final Array_Type arrayType = (Array_Type) type;
			final StringBuilder sbforTemp = aData.getCodeForType(arrayType.getGenNameOwn());
			arrayType.generateCodeValue(aData, sbforTemp);
		}

		if (getMyScope() instanceof ComponentTypeBody) {
			source.append(MessageFormat.format("ThreadLocal<{0}> {1} = new ThreadLocal<{0}>() '{'\n", typeGeneratedName, genName));
			source.append("@Override\n" );
			source.append(MessageFormat.format("protected {0} initialValue() '{'\n", typeGeneratedName));
			source.append(MessageFormat.format("return new {0}();\n", typeGeneratedName));
			source.append("}\n");
			source.append("};\n");
			sb.append(source);
			if ( initialValue != null ) {
				initialValue.generateCodeInit(aData, initComp, genName + ".get()" );
			} else if (cleanUp) {
				initComp.append(genName);
				initComp.append(".get().clean_up();\n");
			}
		} else {
			source.append(MessageFormat.format("{0} {1} = new {0}();\n", typeGeneratedName, genName));
			sb.append(source);
			if ( initialValue != null ) {
				initialValue.generateCodeInit(aData, initComp, genName );
			} else if (cleanUp) {
				initComp.append(genName);
				initComp.append(".clean_up();\n");
			}
		}
	}

	@Override
	/** {@inheritDoc} */
	public void generateCodeString(final JavaGenData aData, final StringBuilder source) {
		final String genName = getGenName();

		final String typeGeneratedName = type.getGenNameValue( aData, source );
		if (type.getTypetype() == Type_type.TYPE_ARRAY) {
			final Array_Type arrayType = (Array_Type) type;
			final StringBuilder sb = aData.getCodeForType(arrayType.getGenNameOwn());
			arrayType.generateCodeValue(aData, sb);
			arrayType.generateCodeTemplate(aData, sb);
		}

		if (initialValue != null && initialValue.canGenerateSingleExpression() ) {
			final ExpressionStruct expression = new ExpressionStruct();
			initialValue.generateCodeExpressionMandatory(aData, expression, false);

			if (initialValue.returnsNative() || initialValue.getValuetype() == Value_type.REFERENCED_VALUE
					|| initialValue.getValuetype() == Value_type.UNDEFINED_LOWERIDENTIFIER_VALUE
					|| type.getTypetypeTtcn3() != initialValue.getExpressionReturntype(CompilationTimeStamp.getBaseTimestamp(), Expected_Value_type.EXPECTED_TEMPLATE)) {
				//TODO if the referenced value is an external function we don't need to make a copy
				source.append(MessageFormat.format("final {0} {1} = new {0}({2});\n", typeGeneratedName, genName, expression.expression));
			} else {
				source.append(MessageFormat.format("final {0} {1} = {2};\n", typeGeneratedName, genName, expression.expression));
			}
		} else {
			source.append(MessageFormat.format("final {0} {1} = new {0}();\n", typeGeneratedName, genName));
			if (initialValue != null) {
				initialValue.generateCodeInit(aData, source, genName );
			}
		}
	}

	@Override
	/** {@inheritDoc} */
	public void generateCodeInitComp(final JavaGenData aData, final StringBuilder initComp, final Definition definition) {
		if (initialValue != null) {
			initialValue.generateCodeInit(aData, initComp, definition.getGenNameFromScope(aData, initComp, ""));
		}
	}
}
