package org.nikolasparaskakis.core;



/**
 * These values represent all alternative positions of individuals or literals in an ABox axiom:
 *  SINGLE the presents the unique position for an ABox axiom with a single individual or literal (e.g. class assertion).
 *  SUBJECT and OBJECT represent the individual/literal position of an ABox axiom with two individuals/literals (e.g. a property assertion)
 */
@SuppressWarnings({"unused", "DuplicatedCode"})
public enum Position {
    SINGLE,
    SUBJECT,
    OBJECT
}