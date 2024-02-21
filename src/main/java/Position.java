/**
 * These values represent all alternative positions of individuals or literals in an Abox axiom:
 *  SINGLE the presents the unique position for an Abox axiom with a single individual or literal (e.g. class assertion).
 *  SUBJECT and OBJECT represent the individual/literal position of an Abox axiom with two individuals/literals (e.g. a property assertion)
 */
public enum Position {
    SINGLE,
    SUBJECT,
    OBJECT
}
