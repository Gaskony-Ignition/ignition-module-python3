package com.gaskony.python3.designer.managers;

import com.gaskony.python3.designer.SavedScript;
import com.gaskony.python3.designer.ScriptMetadata;
import com.gaskony.python3.designer.ScriptTreeNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import javax.swing.tree.DefaultTreeModel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for the static, pure-logic helpers extracted into
 * {@link Python3IDEScriptOps}. The instance methods that drive Swing dialogs
 * are not covered here &mdash; those are wired directly to {@code DarkDialog}
 * and require an interactive Swing display.
 */
class Python3IDEScriptOpsTest {

    @ParameterizedTest
    @ValueSource(strings = {
        "myScript", "my_script", "my-script", "MyScript", "Script1", "script.py",
        "a", "longerNameWithMixed_chars"
    })
    void isValidName_acceptsLegalNames(String name) {
        assertTrue(Python3IDEScriptOps.isValidName(name),
            "Expected '" + name + "' to be valid");
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "name/with/slash",
        "name\\with\\backslash",
        "name:with:colon",
        "name*with*star",
        "name?with?qmark",
        "name\"with\"quote",
        "name<with<lt",
        "name>with>gt",
        "name|with|pipe"
    })
    void isValidName_rejectsIllegalChars(String name) {
        assertFalse(Python3IDEScriptOps.isValidName(name),
            "Expected '" + name + "' to be invalid");
    }

    @Test
    void isValidName_rejectsNullAndBlank() {
        assertFalse(Python3IDEScriptOps.isValidName(null));
        assertFalse(Python3IDEScriptOps.isValidName(""));
        assertFalse(Python3IDEScriptOps.isValidName("   "));
        assertFalse(Python3IDEScriptOps.isValidName("\t"));
    }

    @Test
    void getFolderPathForNode_rootIsEmpty() {
        ScriptTreeNode root = new ScriptTreeNode("Scripts");
        assertEquals("", Python3IDEScriptOps.getFolderPathForNode(root, root));
    }

    @Test
    void getFolderPathForNode_singleLevel() {
        ScriptTreeNode root = new ScriptTreeNode("Scripts");
        ScriptTreeNode folder = new ScriptTreeNode("utils");
        root.add(folder);
        assertEquals("utils", Python3IDEScriptOps.getFolderPathForNode(folder, root));
    }

    @Test
    void getFolderPathForNode_nested() {
        ScriptTreeNode root = new ScriptTreeNode("Scripts");
        ScriptTreeNode parent = new ScriptTreeNode("utils");
        ScriptTreeNode child = new ScriptTreeNode("string");
        ScriptTreeNode grand = new ScriptTreeNode("ascii");
        root.add(parent);
        parent.add(child);
        child.add(grand);
        assertEquals("utils", Python3IDEScriptOps.getFolderPathForNode(parent, root));
        assertEquals("utils/string", Python3IDEScriptOps.getFolderPathForNode(child, root));
        assertEquals("utils/string/ascii", Python3IDEScriptOps.getFolderPathForNode(grand, root));
    }

    @Test
    void collectFolderPaths_skipsScriptNodes() {
        ScriptTreeNode root = new ScriptTreeNode("Scripts");
        ScriptTreeNode utils = new ScriptTreeNode("utils");
        ScriptTreeNode utilsString = new ScriptTreeNode("string");
        ScriptTreeNode services = new ScriptTreeNode("services");
        ScriptMetadata scriptMeta = makeScriptMeta("hello", null);
        ScriptTreeNode scriptNode = new ScriptTreeNode(scriptMeta);

        root.add(utils);
        utils.add(utilsString);
        root.add(services);
        root.add(scriptNode);

        List<String> folders = new ArrayList<>();
        Python3IDEScriptOps.collectFolderPaths(root, "", folders);

        assertEquals(3, folders.size());
        assertTrue(folders.contains("utils"));
        assertTrue(folders.contains("utils/string"));
        assertTrue(folders.contains("services"));
    }

    @Test
    void getOrCreateFolder_buildsHierarchy() {
        ScriptTreeNode root = new ScriptTreeNode("Scripts");
        Map<String, ScriptTreeNode> folders = new HashMap<>();

        ScriptTreeNode resolved = Python3IDEScriptOps.getOrCreateFolder(
            "a/b/c", folders, root);

        assertNotNull(resolved);
        assertEquals(1, root.getChildCount(), "root should have one child 'a'");
        assertEquals("a", root.getChildAt(0).toString());
        assertEquals(3, folders.size(), "folders map should contain a, a/b, a/b/c");
        assertTrue(folders.containsKey("a"));
        assertTrue(folders.containsKey("a/b"));
        assertTrue(folders.containsKey("a/b/c"));
    }

    @Test
    void getOrCreateFolder_idempotent() {
        ScriptTreeNode root = new ScriptTreeNode("Scripts");
        Map<String, ScriptTreeNode> folders = new HashMap<>();

        ScriptTreeNode first = Python3IDEScriptOps.getOrCreateFolder(
            "shared/utils", folders, root);
        ScriptTreeNode second = Python3IDEScriptOps.getOrCreateFolder(
            "shared/utils", folders, root);

        assertSame(first, second, "Second call should return the same node");
        assertEquals(2, folders.size());
    }

    @Test
    void buildScriptTree_placesScriptsByFolderPath() {
        ScriptTreeNode root = new ScriptTreeNode("Scripts");
        DefaultTreeModel model = new DefaultTreeModel(root);

        List<ScriptMetadata> scripts = new ArrayList<>();
        scripts.add(makeScriptMeta("rootScript", null));
        scripts.add(makeScriptMeta("blank", ""));
        scripts.add(makeScriptMeta("util1", "utils"));
        scripts.add(makeScriptMeta("util2", "utils"));
        scripts.add(makeScriptMeta("nested", "utils/string"));

        Python3IDEScriptOps.buildScriptTree(scripts, root, model, null, null, null);

        // 2 scripts at root + 1 utils folder
        int rootChildCount = root.getChildCount();
        assertEquals(3, rootChildCount, "root should have 2 scripts + 1 utils folder");

        ScriptTreeNode utilsFolder = null;
        for (int i = 0; i < rootChildCount; i++) {
            ScriptTreeNode child = (ScriptTreeNode) root.getChildAt(i);
            if (!child.isScript() && "utils".equals(child.toString())) {
                utilsFolder = child;
                break;
            }
        }
        assertNotNull(utilsFolder, "Should have created utils folder");
        // utils contains util1, util2, and string subfolder
        assertEquals(3, utilsFolder.getChildCount());
    }

    @Test
    void buildScriptTree_clearsExistingChildren() {
        ScriptTreeNode root = new ScriptTreeNode("Scripts");
        DefaultTreeModel model = new DefaultTreeModel(root);
        root.add(new ScriptTreeNode("stale"));
        root.add(new ScriptTreeNode("alsoStale"));

        Python3IDEScriptOps.buildScriptTree(
            new ArrayList<>(), root, model, null, null, null);

        assertEquals(0, root.getChildCount());
    }

    @Test
    void convertToMetadata_copiesAllFields() {
        SavedScript saved = new SavedScript(
            "id-1", "myScript", "code body",
            "a description", "alice", "2026-01-01", "2026-02-02",
            "folder/path", "1.2.3"
        );

        ScriptMetadata md = Python3IDEScriptOps.convertToMetadata(saved);

        assertEquals("id-1", md.getId());
        assertEquals("myScript", md.getName());
        assertEquals("a description", md.getDescription());
        assertEquals("alice", md.getAuthor());
        assertEquals("2026-01-01", md.getCreatedDate());
        assertEquals("2026-02-02", md.getLastModified());
        assertEquals("folder/path", md.getFolderPath());
        assertEquals("1.2.3", md.getVersion());
    }

    private static ScriptMetadata makeScriptMeta(String name, String folderPath) {
        return new ScriptMetadata(
            "id-" + name, name, "desc", "alice", "2026-01-01", "2026-01-01", folderPath, "1.0");
    }
}
