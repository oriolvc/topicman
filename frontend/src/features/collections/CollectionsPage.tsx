import {
  Alert,
  Anchor,
  Button,
  Center,
  Grid,
  Group,
  Loader,
  NavLink,
  Paper,
  Stack,
  Text,
  Title,
} from '@mantine/core';
import { notifications } from '@mantine/notifications';
import { useState } from 'react';
import { Link, NavLink as RouterNavLink, useNavigate, useParams } from 'react-router';
import type { CollectionResponse, TemplateResponse } from '../../api/collection';
import { ApiProblem } from '../../api/problem';
import { useCollection, useCollections, useCreateCollection } from '../../shared/collections';
import { showProblem } from '../../shared/showProblem';
import { CollectionNameModal } from './CollectionNameModal';
import { useDeleteCollection, useDeleteTemplate, useRenameCollection } from './collectionQueries';
import { ConfirmDeleteModal } from './ConfirmDeleteModal';
import { SendTemplateModal } from './SendTemplateModal';
import { TemplatesTable } from './TemplatesTable';

type Dialog =
  | { kind: 'closed' }
  | { kind: 'create' }
  | { kind: 'rename'; collection: CollectionResponse }
  | { kind: 'deleteCollection'; collection: CollectionResponse }
  | { kind: 'send'; collection: CollectionResponse; template: TemplateResponse }
  | { kind: 'deleteTemplate'; collection: CollectionResponse; template: TemplateResponse };

function templateCount(count: number): string {
  return count === 1 ? '1 template' : `${String(count)} templates`;
}

/** The chosen collection is in the URL (`/collections/<id>`), so it can be bookmarked. */
export function CollectionsPage() {
  const { collectionId } = useParams();
  const selectedId =
    collectionId === undefined || Number.isNaN(Number(collectionId))
      ? undefined
      : Number(collectionId);
  const navigate = useNavigate();
  const collections = useCollections();
  const collection = useCollection(selectedId);
  const create = useCreateCollection();
  const rename = useRenameCollection();
  const removeCollection = useDeleteCollection();
  const removeTemplate = useDeleteTemplate();
  const [dialog, setDialog] = useState<Dialog>({ kind: 'closed' });
  const close = () => {
    setDialog({ kind: 'closed' });
  };

  const deleteCollection = (target: CollectionResponse) => {
    removeCollection.mutate(target.id, {
      onSuccess: () => {
        notifications.show({ color: 'green', message: `Collection ${target.name} deleted` });
        close();
        void navigate('/collections');
      },
      onError: (error) => {
        showProblem(error, 'The collection could not be deleted');
      },
    });
  };

  const deleteTemplate = (target: CollectionResponse, template: TemplateResponse) => {
    removeTemplate.mutate(
      { collectionId: target.id, templateId: template.id },
      {
        onSuccess: () => {
          notifications.show({ color: 'green', message: `Template ${template.name} deleted` });
          close();
        },
        onError: (error) => {
          showProblem(error, 'The template could not be deleted');
        },
      },
    );
  };

  return (
    <Stack>
      <Group justify="space-between">
        <Title order={2}>Collections</Title>
        <Button
          onClick={() => {
            setDialog({ kind: 'create' });
          }}
        >
          New collection
        </Button>
      </Group>
      {collections.isError && (
        <Alert color="red" title="The collections could not be loaded">
          {collections.error instanceof ApiProblem
            ? collections.error.message
            : 'TopicMan could not be reached. Check that it is running.'}
        </Alert>
      )}
      <Grid>
        <Grid.Col span={{ base: 12, md: 4 }}>
          <Paper withBorder p="xs">
            <nav aria-label="Collections">
              {collections.isPending && (
                <Center py="md">
                  <Loader size="sm" aria-label="Loading collections" />
                </Center>
              )}
              {collections.isSuccess && collections.data.length === 0 && (
                <Text c="dimmed" size="sm" p="xs">
                  No collections yet.
                </Text>
              )}
              {collections.data?.map((summary) => (
                <NavLink
                  key={summary.id}
                  component={RouterNavLink}
                  to={`/collections/${String(summary.id)}`}
                  label={summary.name}
                  description={templateCount(summary.templateCount)}
                />
              ))}
            </nav>
          </Paper>
        </Grid.Col>
        <Grid.Col span={{ base: 12, md: 8 }}>
          {selectedId === undefined && (
            <Text c="dimmed">Choose a collection, or create one to save message templates.</Text>
          )}
          {selectedId !== undefined && collection.isPending && (
            <Center py="xl">
              <Loader aria-label="Loading collection" />
            </Center>
          )}
          {collection.isError && (
            <Alert color="red" title="The collection could not be loaded">
              {collection.error instanceof ApiProblem
                ? collection.error.message
                : 'TopicMan could not be reached. Check that it is running.'}
            </Alert>
          )}
          {selectedId !== undefined && collection.isSuccess && (
            <Stack>
              <Group justify="space-between">
                <Title order={3}>{collection.data.name}</Title>
                <Group gap="xs">
                  <Button
                    variant="default"
                    size="xs"
                    onClick={() => {
                      setDialog({ kind: 'rename', collection: collection.data });
                    }}
                  >
                    Rename
                  </Button>
                  <Button
                    variant="subtle"
                    color="red"
                    size="xs"
                    onClick={() => {
                      setDialog({ kind: 'deleteCollection', collection: collection.data });
                    }}
                  >
                    Delete collection
                  </Button>
                </Group>
              </Group>
              {collection.data.templates.length === 0 ? (
                <Text c="dimmed">
                  No templates yet.{' '}
                  <Anchor component={Link} to="/produce">
                    Save one from the Produce screen
                  </Anchor>
                  .
                </Text>
              ) : (
                <TemplatesTable
                  collectionId={collection.data.id}
                  templates={collection.data.templates}
                  onSend={(template) => {
                    setDialog({ kind: 'send', collection: collection.data, template });
                  }}
                  onDelete={(template) => {
                    setDialog({ kind: 'deleteTemplate', collection: collection.data, template });
                  }}
                />
              )}
            </Stack>
          )}
        </Grid.Col>
      </Grid>
      {dialog.kind === 'create' && (
        <CollectionNameModal
          title="New collection"
          submitLabel="Create"
          initialName=""
          saving={create.isPending}
          onClose={close}
          onSubmit={async (name) => {
            const created = await create.mutateAsync(name);
            notifications.show({ color: 'green', message: `Collection ${created.name} created` });
            close();
            await navigate(`/collections/${String(created.id)}`);
          }}
        />
      )}
      {dialog.kind === 'rename' && (
        <CollectionNameModal
          title="Rename collection"
          submitLabel="Save"
          initialName={dialog.collection.name}
          saving={rename.isPending}
          onClose={close}
          onSubmit={async (name) => {
            await rename.mutateAsync({ id: dialog.collection.id, name });
            close();
          }}
        />
      )}
      {dialog.kind === 'deleteCollection' && (
        <ConfirmDeleteModal
          title="Delete collection"
          deleting={removeCollection.isPending}
          onClose={close}
          onConfirm={() => {
            deleteCollection(dialog.collection);
          }}
        >
          Delete <strong>{dialog.collection.name}</strong> and its{' '}
          {templateCount(dialog.collection.templates.length)}?
        </ConfirmDeleteModal>
      )}
      {dialog.kind === 'deleteTemplate' && (
        <ConfirmDeleteModal
          title="Delete template"
          deleting={removeTemplate.isPending}
          onClose={close}
          onConfirm={() => {
            deleteTemplate(dialog.collection, dialog.template);
          }}
        >
          Delete the template <strong>{dialog.template.name}</strong>?
        </ConfirmDeleteModal>
      )}
      {dialog.kind === 'send' && (
        <SendTemplateModal
          collectionId={dialog.collection.id}
          template={dialog.template}
          onClose={close}
        />
      )}
    </Stack>
  );
}
