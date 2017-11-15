package com.athaydes.gradle.osgi

import com.athaydes.gradle.osgi.bnd.BndWrapper
import com.athaydes.gradle.osgi.util.JarUtils
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.file.FileTreeElement
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.bundling.Jar

import static com.athaydes.gradle.osgi.OsgiRunPlugin.WRAP_EXTENSION

/**
 * The createBundlesDir task.
 */
class CreateBundlesDir extends DefaultTask {

    static final Logger log = Logging.getLogger( CreateBundlesDir )

    @InputFile
    File getBuildFile() {
        log.debug( "Adding project build file to createBundlesDir task inputs: {}", project.buildFile )
        project.buildFile
    }

    @InputFiles
    Set<File> getAllFileInputsFromProjectDependencies() {
        def osgiConfig = project.extensions.getByName( 'runOsgi' ) as OsgiConfig

        def allProjectDeps = ConfigurationsCreator.allRuntimeDependencies( project, osgiConfig ).findAll {
            it instanceof Project
        } as List<Project>

        log.debug "Adding build file and jars of the following projects to the inputs of the " +
                "createBundlesDir task: {}", allProjectDeps*.name

        Set<File> projectDependencies = [ ]

        ( allProjectDeps + project ).collectMany { dep ->
            dep.tasks.withType( Jar ) { Jar jar ->
                // we need to run the jar task if the build file changes
                if ( dep.buildFile ) projectDependencies += dep.buildFile
                // run our task if the jar of any dependency changes
                projectDependencies += jar.outputs.files
            }
        }

        return projectDependencies
    }

    @OutputDirectory
    File getOutputDir() {
        def osgiConfig = project.extensions.getByName( 'runOsgi' ) as OsgiConfig
        project.file( CreateOsgiRuntimeTask.getTarget( project, osgiConfig ) )
    }

    @TaskAction
    void createOsgiRuntime() {
        def osgiConfig = project.extensions.getByName( 'runOsgi' ) as OsgiConfig

        String target = CreateOsgiRuntimeTask.getTarget( project, osgiConfig )

        copyBundles( project, osgiConfig, target )
    }

    private void copyBundles( Project project, OsgiConfig osgiConfig, String target ) {
        def bundlesDir = "${target}/${osgiConfig.bundlesPath}"

        log.info( "Copying OSGi bundles to {}", bundlesDir )

        //noinspection GroovyAssignabilityCheck
        def allDeps = project.configurations.findAll { it.name.startsWith( ConfigurationsCreator.OSGI_DEP_PREFIX ) }

        copyJarsWrappingIfNeeded( project, osgiConfig, allDeps, bundlesDir )
    }

    static void copyJarsWrappingIfNeeded( Project project, OsgiConfig osgiConfig, source, String destination ) {
        def wrapInstructions = osgiConfig[ WRAP_EXTENSION ] as WrapInstructionsConfig
        def nonBundles = [ ] as Set
        def systemLibs = project.configurations.systemLib.resolvedConfiguration
                .resolvedArtifacts.collect { it.file.name } as Set

        project.copy {
            from source
            into destination
            exclude { FileTreeElement element ->
                def inSystemLibs = element.file.name in systemLibs
                def explicityExcluded = osgiConfig.excludedBundles.any { element.name ==~ it }
                if ( inSystemLibs || explicityExcluded ) {
                    def reason = ( inSystemLibs && explicityExcluded ) ?
                            'both explicitly excluded and in system libs' : ( inSystemLibs ?
                            'in system libs' : 'explicitly excluded' )
                    log.info( 'Excluding bundle from bundles directory ({}): {}', reason, element.name )
                    return true
                }
                def nonBundle = JarUtils.notBundle( element.file )
                if ( nonBundle ) nonBundles << element.file
                return nonBundle
            }
        }

        if ( wrapInstructions.enabled ) {
            nonBundles.each { File file ->
                if ( JarUtils.hasManifest( file ) ) {
                    try {
                        BndWrapper.wrapNonBundle( file, destination, wrapInstructions )
                    } catch ( e ) {
                        log.warn( "Unable to wrap ${file.name}", e )
                    }
                } else {
                    log.warn( 'Jar without manifest found, unable to wrap it into a bundle: {}', file.name )
                }
            }
        } else if ( nonBundles ) {
            log.info "The following jars were kept out of the classpath " +
                    "as they are not bundles (enable wrapping if they are needed): {}", nonBundles
        }
    }

}
