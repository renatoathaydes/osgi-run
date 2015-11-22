package com.athaydes.osgi.ds.trie;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import java.util.Collections;
import java.util.List;

import static java.util.Collections.singletonList;

public class ClassTrie<T> {

    private static class Node<T> {
        final List<TypeNode<T>> children = Lists.newArrayListWithCapacity( 2 );

        @Override
        public String toString() {
            return "Root{" +
                    "childrenCount=" + children.size() +
                    '}';
        }
    }

    private static class TypeNode<T> extends Node<T> {
        final Class<?> type;
        final List<T> values = Lists.newArrayListWithCapacity( 2 );

        TypeNode( Class<?> type ) {
            this.type = type;
        }

        @Override
        public String toString() {
            return "Node{" +
                    "type=" + type +
                    ", values=" + values +
                    '}';
        }
    }

    private final Node<T> root = new Node<>();

    public List<T> get( Class<?> typeKey ) {
        TypeNode<T> node = findNode( root, typeHierachy( typeKey ), false );
        if ( node == null ) {
            return Collections.emptyList();
        }
        Iterable<TypeNode<T>> allNodes = descendantsAnd( singletonList( node ) );
        return unwrapValues( allNodes );
    }

    public void put( Class<?> typeKey, T item ) {
        TypeNode<T> node = findNode( root, typeHierachy( typeKey ), true );
        if ( node == null ) {
            throw new IllegalStateException( "Unable to create node for new item" );
        }
        node.values.add( item );
    }

    private TypeNode<T> findNode( Node<T> parent,
                                  Iterable<Class<?>> typeHierarchy,
                                  boolean createIfNotFound ) {
        Class<?> type = Iterables.getFirst( typeHierarchy, null );
        if ( type != null ) {
            Iterable<Class<?>> subtypes = Iterables.skip( typeHierarchy, 1 );
            for (TypeNode<T> child : parent.children) {
                if ( child.type.isAssignableFrom( type ) ) {
                    if ( Iterables.isEmpty( subtypes ) ) {
                        return child;
                    }
                    return findNode( child, subtypes, createIfNotFound );
                }
            }
        }

        if ( createIfNotFound ) {
            return createBranch( parent, typeHierarchy );
        }
        return null;
    }

    private TypeNode<T> createBranch( Node<T> parent, Iterable<Class<?>> typeHierarchy ) {
        if ( Iterables.isEmpty( typeHierarchy ) ) {
            if ( parent instanceof TypeNode ) {
                return ( ( TypeNode<T> ) parent );
            } else {
                throw new IllegalStateException( "Cannot create branch for empty type hierarchy" );
            }
        } else {
            Class<?> type = Iterables.getFirst( typeHierarchy, null );
            TypeNode<T> node = new TypeNode<>( type );
            parent.children.add( node );
            return createBranch( node, Iterables.skip( typeHierarchy, 1 ) );
        }
    }

    private Iterable<TypeNode<T>> descendantsAnd( List<TypeNode<T>> nodes ) {
        if ( nodes.isEmpty() ) {
            return Collections.emptySet();
        }

        List<TypeNode<T>> allChildren = Lists.newArrayList();
        for (Node<T> node : nodes) {
            allChildren.addAll( node.children );
        }

        return Iterables.concat( nodes, descendantsAnd( allChildren ) );
    }

    private List<T> unwrapValues( Iterable<TypeNode<T>> nodes ) {
        List<T> result = Lists.newArrayList();
        for (TypeNode<T> node : nodes) {
            result.addAll( node.values );
        }
        return result;
    }

    private List<Class<?>> typeHierachy( Class<?> type ) {
        List<Class<?>> result = Lists.newArrayListWithCapacity( 4 );
        while ( type != null ) {
            result.add( type );
            type = type.getSuperclass();
        }
        Collections.reverse( result );
        return result;
    }

    private void buildString( StringBuilder builder, String indent,
                              List<? extends Node<T>> nodes ) {
        for (Node<T> node : nodes) {
            builder.append( indent )
                    .append( node ).append( '\n' );
            buildString( builder, indent + "  ", node.children );
        }
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        buildString( builder, "  ", singletonList( root ) );
        return "ClassTrie {\n" + builder.toString() +
                '}';
    }
}
