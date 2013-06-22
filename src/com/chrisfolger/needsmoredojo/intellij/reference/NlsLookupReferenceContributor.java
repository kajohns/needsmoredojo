package com.chrisfolger.needsmoredojo.intellij.reference;

import com.chrisfolger.needsmoredojo.core.amd.DefineResolver;
import com.intellij.javascript.JavaScriptReferenceContributor;
import com.intellij.lang.javascript.psi.*;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.patterns.ElementPattern;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.*;
import com.intellij.psi.search.FilenameIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.PatternUtil;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class NlsLookupReferenceContributor extends JavaScriptReferenceContributor
{
    @Override
    public void registerReferenceProviders(PsiReferenceRegistrar registrar)
    {
        ElementPattern<JSLiteralExpression> pattern = PlatformPatterns.psiElement(JSLiteralExpression.class);

        registrar.registerReferenceProvider(pattern, new PsiReferenceProvider() {
            @NotNull
            @Override
            public PsiReference[] getReferencesByElement(@NotNull PsiElement psiElement, @NotNull ProcessingContext processingContext) {
                PsiElement parent = psiElement.getParent();
                if(parent instanceof JSIndexedPropertyAccessExpression) {
                    final JSIndexedPropertyAccessExpression accessor = (JSIndexedPropertyAccessExpression) parent;
                    final PsiElement qualifier = accessor.getQualifier();

                    // get the list of defines
                    // find one that matches
                    // check to see if it's an i18n file
                    // resolve the reference to the file
                    List<PsiElement> defines = new ArrayList<PsiElement>();
                    List<PsiElement> parameters = new ArrayList<PsiElement>();
                    new DefineResolver().gatherDefineAndParameters(qualifier.getContainingFile(), defines, parameters);

                    PsiElement correctDefine = null;
                    for(int i=0;i<parameters.size();i++)
                    {
                        if(parameters.get(i).getText().equals(qualifier.getText()))
                        {
                            correctDefine = defines.get(i);
                        }
                    }

                    String defineText = correctDefine.getText();
                    defineText = defineText.substring(defineText.lastIndexOf("!") + 1).replaceAll("'", "");

                    // TODO find relative path etc.
                    PsiFile[] files = FilenameIndex.getFilesByName(correctDefine.getProject(), "dojo.js", GlobalSearchScope.projectScope(correctDefine.getProject()));
                    PsiFile dojoFile = null;

                    for(PsiFile file : files)
                    {
                        if(file.getContainingDirectory().getName().equals("dojo"))
                        {
                            dojoFile = file;
                            break;
                        }
                    }

                    VirtualFile i18nFile = dojoFile.getContainingDirectory().getParent().getVirtualFile().findFileByRelativePath("/" + defineText + ".js");
                    PsiFile templateFile = PsiManager.getInstance(dojoFile.getProject()).findFile(i18nFile);

                    final PsiElement[] i18nElement = {null};
                    templateFile.acceptChildren(new JSRecursiveElementVisitor() {
                        @Override
                        public void visitJSObjectLiteralExpression(JSObjectLiteralExpression node)
                        {
                            for(JSProperty property : node.getProperties())
                            {
                                String propertyText = accessor.getIndexExpression().getText();
                                propertyText = propertyText.substring(1, propertyText.length() - 1);

                                if(property.getName().equals(propertyText))
                                {
                                    i18nElement[0] = property;
                                }
                            }

                            super.visitJSObjectLiteralExpression(node);
                        }
                    });

                    return new PsiReference[] { new NlsLookupReference((JSLiteralExpression) psiElement, i18nElement[0]) };
                }

                return new PsiReference[0];  //To change body of implemented methods use File | Settings | File Templates.
            }
        });
    }
}
